package ${package};

import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("MyViewController")
public class MyViewController extends UIViewController {
    @IBOutlet
    private UILabel label;
    private int clickCount;

    @IBAction
    private void clicked() {
        label.setText("Click Nr. " + (++clickCount));
    }
}
