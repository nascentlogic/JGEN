package org.example;

import io.github.nascentlogic.jgen.gui.JuiCore;
import io.github.nascentlogic.jgen.gui.debug.DebugWindow;
import io.github.nascentlogic.jgen.utils.JgenUtils;
import org.joml.primitives.Rectanglef;

import java.util.ArrayList;
import java.util.List;

import static io.github.nascentlogic.jgen.gui.debug.DebugLayout.*;


/**
 * F.Dahl, 10/11/2026
 */
public class WindowTest extends DebugWindow {

    private final List<String> listItems = new ArrayList<>();
    int selectedItem = -1;
    int removeIndex = -1;


    public WindowTest(int contentWidth, int contentHeight, String name) {
        super(contentWidth, contentHeight, name);
        listItems.add("Short Sword +2");
        listItems.add("Broken Sword");
        listItems.add("Parry Shield");
        listItems.add("Half-Plate Armor");
        listItems.add("Studded Leather Armor");
        listItems.add("Moonstone");
        listItems.add("Rapier +1");
        listItems.add("Meat");
        listItems.add("Cocking Materials");
        listItems.add("Glint Stone");
        listItems.add("Scythe");
        listItems.add("Farming Equipment");
        listItems.add("Fire Seeds");
        listItems.add("Hockey Mask +5");
    }

    public void processContent(Rectanglef contentArea, JuiCore jui, int bgID, float dt) {

        if (removeIndex != -1) {
            listItems.remove(removeIndex);
            removeIndex = -1;
        }

        beginAbsolute(jui,contentArea);
        beginScrollContainer(jui,jui.currentContainer().availableSpace()); // todo: make a remaineing version

        if (beginGroup(jui,"Statistics")) {

            if (beginGroup(jui,"Game Stats")) {

                for (int i = 0; i < 30; i++) {
                    if (beginGroup(jui,"Menu " + i)) {
                        Rectanglef rect = JgenUtils.pushRect();
                        jui.allocate(64,rect);
                        JgenUtils.popRect();
                    } endGroup(jui);
                }

            } endGroup(jui);

            if (beginGroup(jui,"Core Stats")) {


                beginList(jui,"list",7);
                int i = 0;
                for (String s : listItems) {
                    int code = listItem(jui,s,(i == selectedItem));
                    if (code == 1) selectedItem = i;
                    else if (code == 2) {
                        removeIndex = i;
                    }
                    i++;
                }
                endList(jui);



            } endGroup(jui);

            if (beginGroup(jui,"More Stats")) {
                Rectanglef rect = JgenUtils.pushRect();
                jui.allocate(200,rect);
                JgenUtils.popRect();
            } endGroup(jui);

            if (beginGroup(jui,"Pron")) {
                Rectanglef rect = JgenUtils.pushRect();
                jui.allocate(200,rect);
                JgenUtils.popRect();
            } endGroup(jui);

        } endGroup(jui);

        endScrollContainer(jui);
        endAbsolute(jui);
    }
}
