package ${package};

import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.objc.annotation.CustomClass;
import org.robovm.objc.annotation.IBAction;
import org.robovm.objc.annotation.IBOutlet;

@CustomClass("MyViewController")
public class MyViewController extends UIViewController {
    private static CounterStore counterStore = new CounterStore();

    @IBOutlet
    private UILabel label;

    @IBAction
    private void clicked() {
        counterStore.add(1);
        label.setText("Click Nr. " + counterStore.get());
    }
}
