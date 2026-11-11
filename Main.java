
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {
    private long window;
    private final int WIDTH = 1280;
    private final int HEIGHT = 720;

    private int vaoID, vboID, eboID;
    private int shaderProgram;

    // Toggle Kontrol Interaktif (Spesifikasi Tugas)
    private boolean useBlinnPhong = true;
    private boolean useFlatShading = false;
    private boolean enableDepthTest = true;
    private boolean enableCulling = true;

    // Posisi Kamera & Cahaya
    private Vector3f cameraPos = new Vector3f(0.0f, 2.0f, 5.0f);
    private Vector3f lightPos  = new Vector3f(2.0f, 4.0f, 2.0f);

    public void run() {
        init();
        loop();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "Tugas Grafkom Minggu 11 - 3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window GLFW");

        // Input Keyboard Handler (Indikator Tugas 1, 2, 3)
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_B) {
                    useBlinnPhong = !useBlinnPhong;
                    System.out.println("Mode Specular: " + (useBlinnPhong ? "Blinn-Phong" : "Phong"));
                }
                if (key == GLFW_KEY_F) {
                    useFlatShading = !useFlatShading;
                    System.out.println("Mode Shading: " + (useFlatShading ? "Flat" : "Smooth"));
                }
                if (key == GLFW_KEY_Z) {
                    enableDepthTest = !enableDepthTest;
                    HSRManager.setupDepthTest(enableDepthTest);
                    System.out.println("Z-Buffer Depth Test: " + (enableDepthTest ? "ON" : "OFF"));
                }
                if (key == GLFW_KEY_C) {
                    enableCulling = !enableCulling;
                    HSRManager.setupBackFaceCulling(enableCulling);
                    System.out.println("Back-Face Culling: " + (enableCulling ? "ON" : "OFF"));
                }
            }
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // V-Sync
        glfwShowWindow(window);

        GL.createCapabilities();

        // Inisialisasi HSR Manager awal
        HSRManager.setupDepthTest(enableDepthTest);
        HSRManager.setupBackFaceCulling(enableCulling);

        // Buat Geometri Mesh 3D (Kubus 3D)
        setupCubeMesh();

        // Load Shaders phong.vert & phong.frag
        setupShaders();
    }

    private void setupCubeMesh() {
        // Vertex Array: Posisi (3f), Normal (3f), UV (2f)
        float[] vertices = {
            // Depan
            -0.5f,-0.5f, 0.5f,   0.0f, 0.0f, 1.0f,   0.0f, 0.0f,
             0.5f,-0.5f, 0.5f,   0.0f, 0.0f, 1.0f,   1.0f, 0.0f,
             0.5f, 0.5f, 0.5f,   0.0f, 0.0f, 1.0f,   1.0f, 1.0f,
            -0.5f, 0.5f, 0.5f,   0.0f, 0.0f, 1.0f,   0.0f, 1.0f,
            // Belakang
            -0.5f,-0.5f,-0.5f,   0.0f, 0.0f,-1.0f,   1.0f, 0.0f,
            -0.5f, 0.5f,-0.5f,   0.0f, 0.0f,-1.0f,   1.0f, 1.0f,
             0.5f, 0.5f,-0.5f,   0.0f, 0.0f,-1.0f,   0.0f, 1.0f,
             0.5f,-0.5f,-0.5f,   0.0f, 0.0f,-1.0f,   0.0f, 0.0f,
            // Atas
            -0.5f, 0.5f,-0.5f,   0.0f, 1.0f, 0.0f,   0.0f, 1.0f,
            -0.5f, 0.5f, 0.5f,   0.0f, 1.0f, 0.0f,   0.0f, 0.0f,
             0.5f, 0.5f, 0.5f,   0.0f, 1.0f, 0.0f,   1.0f, 0.0f,
             0.5f, 0.5f,-0.5f,   0.0f, 1.0f, 0.0f,   1.0f, 1.0f,
            // Bawah
            -0.5f,-0.5f,-0.5f,   0.0f,-1.0f, 0.0f,   1.0f, 1.0f,
             0.5f,-0.5f,-0.5f,   0.0f,-1.0f, 0.0f,   0.0f, 1.0f,
             0.5f,-0.5f, 0.5f,   0.0f,-1.0f, 0.0f,   0.0f, 0.0f,
            -0.5f,-0.5f, 0.5f,   0.0f,-1.0f, 0.0f,   1.0f, 0.0f,
            // Kanan
             0.5f,-0.5f,-0.5f,   1.0f, 0.0f, 0.0f,   1.0f, 0.0f,
             0.5f, 0.5f,-0.5f,   1.0f, 0.0f, 0.0f,   1.0f, 1.0f,
             0.5f, 0.5f, 0.5f,   1.0f, 0.0f, 0.0f,   0.0f, 1.0f,
             0.5f,-0.5f, 0.5f,   1.0f, 0.0f, 0.0f,   0.0f, 0.0f,
            // Kiri
            -0.5f,-0.5f,-0.5f,  -1.0f, 0.0f, 0.0f,   0.0f, 0.0f,
            -0.5f,-0.5f, 0.5f,  -1.0f, 0.0f, 0.0f,   1.0f, 0.0f,
            -0.5f, 0.5f, 0.5f,  -1.0f, 0.0f, 0.0f,   1.0f, 1.0f,
            -0.5f, 0.5f,-0.5f,  -1.0f, 0.0f, 0.0f,   0.0f, 1.0f
        };

        int[] indices = {
             0, 1, 2,  2, 3, 0,
             4, 5, 6,  6, 7, 4,
             8, 9,10, 10,11, 8,
            12,13,14, 14,15,12,
            16,17,18, 18,19,16,
            20,21,22, 22,23,20
        };

        vaoID = glGenVertexArrays();
        vboID = glGenBuffers();
        eboID = glGenBuffers();

        glBindVertexArray(vaoID);

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        // Position attribute (0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // Normal attribute (1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        // TexCoords attribute (2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    private void setupShaders() {
        String vertCode = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;
            layout (location = 2) in vec2 aTexCoords;
            out vec3 FragPos;
            out vec3 Normal;
            out vec2 TexCoords;
            uniform mat4 model;
            uniform mat4 view;
            uniform mat4 projection;
            void main() {
                FragPos = vec3(model * vec4(aPos, 1.0));
                Normal = mat3(transpose(inverse(model))) * aNormal;
                TexCoords = aTexCoords;
                gl_Position = projection * view * vec4(FragPos, 1.0);
            }
            """;

        String fragCode = """
            #version 330 core
            out vec4 FragColor;
            in vec3 FragPos;
            in vec3 Normal;
            in vec2 TexCoords;
            uniform vec3 lightPos;
            uniform vec3 viewPos;
            uniform vec3 lightColor;
            uniform vec3 objectColor;
            uniform bool useBlinnPhong;
            void main() {
                float ambientStrength = 0.2;
                vec3 ambient = ambientStrength * lightColor;
                vec3 norm = normalize(Normal);
                vec3 lightDir = normalize(lightPos - FragPos);
                float diff = max(dot(norm, lightDir), 0.0);
                vec3 diffuse = diff * lightColor;
                float specularStrength = 0.8;
                vec3 viewDir = normalize(viewPos - FragPos);
                float spec = 0.0;
                if (useBlinnPhong) {
                    vec3 halfwayDir = normalize(lightDir + viewDir);
                    spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
                } else {
                    vec3 reflectDir = reflect(-lightDir, norm);
                    spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
                }
                vec3 specular = specularStrength * spec * lightColor;
                vec3 result = (ambient + diffuse + specular) * objectColor;
                FragColor = vec4(result, 1.0);
            }
            """;

        int vertShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertShader, vertCode);
        glCompileShader(vertShader);

        int fragShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragShader, fragCode);
        glCompileShader(fragShader);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertShader);
        glAttachShader(shaderProgram, fragShader);
        glLinkProgram(shaderProgram);

        glDeleteShader(vertShader);
        glDeleteShader(fragShader);
    }

    private void loop() {
        glClearColor(0.1f, 0.15f, 0.2f, 1.0f);

        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(45.0), (float) WIDTH / HEIGHT, 0.1f, 100.0f);
        Matrix4f view = new Matrix4f().lookAt(cameraPos, new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f));

        float rotation = 0.0f;

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            rotation += 0.01f;
            Matrix4f model = new Matrix4f().rotate(rotation, 0.5f, 1.0f, 0.0f);

            glUseProgram(shaderProgram);

            // Pass Uniform Matrix & Lighting
            try (MemoryStack stack = stackPush()) {
                FloatBuffer fbProj = stack.mallocFloat(16);
                FloatBuffer fbView = stack.mallocFloat(16);
                FloatBuffer fbModel = stack.mallocFloat(16);

                glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "projection"), false, proj.get(fbProj));
                glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "view"), false, view.get(fbView));
                glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "model"), false, model.get(fbModel));
            }

            glUniform3f(glGetUniformLocation(shaderProgram, "lightPos"), lightPos.x, lightPos.y, lightPos.z);
            glUniform3f(glGetUniformLocation(shaderProgram, "viewPos"), cameraPos.x, cameraPos.y, cameraPos.z);
            glUniform3f(glGetUniformLocation(shaderProgram, "lightColor"), 1.0f, 1.0f, 1.0f);
            glUniform3f(glGetUniformLocation(shaderProgram, "objectColor"), 0.2f, 0.6f, 1.0f);
            glUniform1i(glGetUniformLocation(shaderProgram, "useBlinnPhong"), useBlinnPhong ? 1 : 0);

            // Render Objek 3D
            glBindVertexArray(vaoID);
            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public static void main(String[] args) {
        new Main().run();
    }
}