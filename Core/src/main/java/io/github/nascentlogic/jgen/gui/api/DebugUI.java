package io.github.nascentlogic.jgen.gui.api;

import io.github.nascentlogic.jgen.gfx.Color;
import org.joml.*;

/**
 * F.Dahl, 10/1/2026
 */
public interface DebugUI {

    // detect whether inside a sub group. color different accordinbgly

    boolean beginGroup(String name); // pop down
    void endGroup();
    void separator();

    void beginButtons(String name); // buttons grop together horizontally if there's room. Then continue by row
    void endButtons();
    boolean button(String label);


    boolean beginMenu(String name, int heightItems);
    boolean menuItem(String label, boolean selected);
    void endMenu();

    void label(String label);
    void labelValue(String label, String value);
    void labelValue(String label, boolean b);
    void labelValue(String label, int i);
    void labelValue(String label, int i1, int i2);
    void labelValue(String label, int i1, int i2, int i3);
    void labelValue(String label, int i1, int i2, int i3, int i4);
    default void labelValue(String label, Vector2i vec2) { labelValue(label,vec2.x,vec2.y); }
    default void labelValue(String label, Vector3i vec3) { labelValue(label,vec3.x,vec3.y,vec3.z); }
    default void labelValue(String label, Vector4i vec4) { labelValue(label,vec4.x,vec4.y,vec4.z,vec4.w); }
    void labelValue(String label, float f);
    void labelValue(String label, float f1, float f2);
    void labelValue(String label, float f1, float f2, float f3);
    void labelValue(String label, float f1, float f2, float f3, float f4);
    default void labelValue(String label, Vector2f vec2) { labelValue(label,vec2.x,vec2.y); }
    default void labelValue(String label, Vector3f vec3) { labelValue(label,vec3.x,vec3.y,vec3.z); }
    default void labelValue(String label, Vector4f vec4) { labelValue(label,vec4.x,vec4.y,vec4.z,vec4.w); }
    void labelValue(String label, Color color);

    float slider(String label, float value, float min, float max, int decimals); // can also use keyboard input
    int slider(String label, int value, int min, int max); // can also use keyboard input

    // log

   // boolean textField(Text text, int heightLines);
   // boolean inputField(TextBlock text);

}
