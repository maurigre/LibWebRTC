package org.webrtc;

import android.opengl.GLES20;
import android.support.annotation.Nullable;
import java.nio.FloatBuffer;

class GlGenericDrawer implements RendererCommon.GlDrawer {
  private static final String INPUT_VERTEX_COORDINATE_NAME = "in_pos";
  
  private static final String INPUT_TEXTURE_COORDINATE_NAME = "in_tc";
  
  private static final String TEXTURE_MATRIX_NAME = "tex_mat";
  
  private static final String DEFAULT_VERTEX_SHADER_STRING = "varying vec2 tc;\nattribute vec4 in_pos;\nattribute vec4 in_tc;\nuniform mat4 tex_mat;\nvoid main() {\n  gl_Position = in_pos;\n  tc = (tex_mat * in_tc).xy;\n}\n";
  
  public static interface ShaderCallbacks {
    void onNewShader(GlShader param1GlShader);
    
    void onPrepareShader(GlShader param1GlShader, float[] param1ArrayOffloat, int param1Int1, int param1Int2, int param1Int3, int param1Int4);
  }
  
  public enum ShaderType {
    OES, RGB, YUV;
  }
  
  private static final FloatBuffer FULL_RECTANGLE_BUFFER = GlUtil.createFloatBuffer(new float[] { -1.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F });
  
  private static final FloatBuffer FULL_RECTANGLE_TEXTURE_BUFFER = GlUtil.createFloatBuffer(new float[] { 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F });
  
  private final String genericFragmentSource;
  
  private final String vertexShader;
  
  private final ShaderCallbacks shaderCallbacks;
  
  @Nullable
  private ShaderType currentShaderType;
  
  @Nullable
  private GlShader currentShader;
  
  private int inPosLocation;
  
  private int inTcLocation;
  
  private int texMatrixLocation;
  
  static String createFragmentShaderString(String genericFragmentSource, ShaderType shaderType) {
    StringBuilder stringBuilder = new StringBuilder();
    if (shaderType == ShaderType.OES)
      stringBuilder.append("#extension GL_OES_EGL_image_external : require\n"); 
    stringBuilder.append("precision mediump float;\n");
    stringBuilder.append("varying vec2 tc;\n");
    if (shaderType == ShaderType.YUV) {
      stringBuilder.append("uniform sampler2D y_tex;\n");
      stringBuilder.append("uniform sampler2D u_tex;\n");
      stringBuilder.append("uniform sampler2D v_tex;\n");
      stringBuilder.append("vec4 sample(vec2 p) {\n");
      stringBuilder.append("  float y = texture2D(y_tex, p).r * 1.16438;\n");
      stringBuilder.append("  float u = texture2D(u_tex, p).r;\n");
      stringBuilder.append("  float v = texture2D(v_tex, p).r;\n");
      stringBuilder.append("  return vec4(y + 1.59603 * v - 0.874202,\n");
      stringBuilder.append("    y - 0.391762 * u - 0.812968 * v + 0.531668,\n");
      stringBuilder.append("    y + 2.01723 * u - 1.08563, 1);\n");
      stringBuilder.append("}\n");
      stringBuilder.append(genericFragmentSource);
    } else {
      String samplerName = (shaderType == ShaderType.OES) ? "samplerExternalOES" : "sampler2D";
      stringBuilder.append("uniform ").append(samplerName).append(" tex;\n");
      stringBuilder.append(genericFragmentSource.replace("sample(", "texture2D(tex, "));
    } 
    return stringBuilder.toString();
  }
  
  public GlGenericDrawer(String genericFragmentSource, ShaderCallbacks shaderCallbacks) {
    this("varying vec2 tc;\nattribute vec4 in_pos;\nattribute vec4 in_tc;\nuniform mat4 tex_mat;\nvoid main() {\n  gl_Position = in_pos;\n  tc = (tex_mat * in_tc).xy;\n}\n", genericFragmentSource, shaderCallbacks);
  }
  
  public GlGenericDrawer(String vertexShader, String genericFragmentSource, ShaderCallbacks shaderCallbacks) {
    this.vertexShader = vertexShader;
    this.genericFragmentSource = genericFragmentSource;
    this.shaderCallbacks = shaderCallbacks;
  }
  
  GlShader createShader(ShaderType shaderType) {
    return new GlShader(this.vertexShader, 
        createFragmentShaderString(this.genericFragmentSource, shaderType));
  }
  
  public void drawOes(int oesTextureId, float[] texMatrix, int frameWidth, int frameHeight, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
    prepareShader(ShaderType.OES, texMatrix, frameWidth, frameHeight, viewportWidth, viewportHeight);
    GLES20.glActiveTexture(33984);
    GLES20.glBindTexture(36197, oesTextureId);
    GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    GLES20.glDrawArrays(5, 0, 4);
    GLES20.glBindTexture(36197, 0);
  }
  
  public void drawRgb(int textureId, float[] texMatrix, int frameWidth, int frameHeight, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
    prepareShader(ShaderType.RGB, texMatrix, frameWidth, frameHeight, viewportWidth, viewportHeight);
    GLES20.glActiveTexture(33984);
    GLES20.glBindTexture(3553, textureId);
    GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    GLES20.glDrawArrays(5, 0, 4);
    GLES20.glBindTexture(3553, 0);
  }
  
  public void drawYuv(int[] yuvTextures, float[] texMatrix, int frameWidth, int frameHeight, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
    prepareShader(ShaderType.YUV, texMatrix, frameWidth, frameHeight, viewportWidth, viewportHeight);
    int i;
    for (i = 0; i < 3; i++) {
      GLES20.glActiveTexture(33984 + i);
      GLES20.glBindTexture(3553, yuvTextures[i]);
    } 
    GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    GLES20.glDrawArrays(5, 0, 4);
    for (i = 0; i < 3; i++) {
      GLES20.glActiveTexture(33984 + i);
      GLES20.glBindTexture(3553, 0);
    } 
  }
  
  private void prepareShader(ShaderType shaderType, float[] texMatrix, int frameWidth, int frameHeight, int viewportWidth, int viewportHeight) {
    GlShader shader;
    if (shaderType.equals(this.currentShaderType)) {
      shader = this.currentShader;
    } else {
      this.currentShaderType = shaderType;
      if (this.currentShader != null)
        this.currentShader.release(); 
      shader = createShader(shaderType);
      this.currentShader = shader;
      shader.useProgram();
      if (shaderType == ShaderType.YUV) {
        GLES20.glUniform1i(shader.getUniformLocation("y_tex"), 0);
        GLES20.glUniform1i(shader.getUniformLocation("u_tex"), 1);
        GLES20.glUniform1i(shader.getUniformLocation("v_tex"), 2);
      } else {
        GLES20.glUniform1i(shader.getUniformLocation("tex"), 0);
      } 
      GlUtil.checkNoGLES2Error("Create shader");
      this.shaderCallbacks.onNewShader(shader);
      this.texMatrixLocation = shader.getUniformLocation("tex_mat");
      this.inPosLocation = shader.getAttribLocation("in_pos");
      this.inTcLocation = shader.getAttribLocation("in_tc");
    } 
    shader.useProgram();
    GLES20.glEnableVertexAttribArray(this.inPosLocation);
    GLES20.glVertexAttribPointer(this.inPosLocation, 2, 5126, false, 0, FULL_RECTANGLE_BUFFER);
    GLES20.glEnableVertexAttribArray(this.inTcLocation);
    GLES20.glVertexAttribPointer(this.inTcLocation, 2, 5126, false, 0, FULL_RECTANGLE_TEXTURE_BUFFER);
    GLES20.glUniformMatrix4fv(this.texMatrixLocation, 1, false, texMatrix, 0);
    this.shaderCallbacks.onPrepareShader(shader, texMatrix, frameWidth, frameHeight, viewportWidth, viewportHeight);
    GlUtil.checkNoGLES2Error("Prepare shader");
  }
  
  public void release() {
    if (this.currentShader != null) {
      this.currentShader.release();
      this.currentShader = null;
      this.currentShaderType = null;
    } 
  }
}
