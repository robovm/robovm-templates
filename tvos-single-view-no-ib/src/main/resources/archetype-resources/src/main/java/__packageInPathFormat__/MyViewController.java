${package}

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewController;

public class MyViewController extends UIViewController {
    private final UIButton button;
    private final UILabel label;
    private int clickCount;

    public MyViewController() {
        // Get the view of this view controller.
        UIView view = getView();

        // Get the bounds of the view.
        double viewWidth = view.getBounds().getWidth();
        double viewHeight = view.getBounds().getHeight();

        // Setup a label. Centered in the top half of the screen.
        double labelWidth = viewWidth;
        double labelHeight = 150;
        double labelX = (viewWidth - labelWidth) / 2.0;
        double labelY = (viewHeight * 1.0 / 4.0) - labelHeight / 2.0;
        label = new UILabel(new CGRect(labelX, labelY, labelWidth, labelHeight));
        label.setFont(UIFont.getSystemFont(40));
        label.setTextAlignment(NSTextAlignment.Center);
        view.addSubview(label);

        // Setup a button. Centered in the bottom half of the screen.
        double buttonWidth = viewWidth - 200;
        double buttonHeight = 150;
        double buttonX = (viewWidth - buttonWidth) / 2.0;
        double buttonY = (viewHeight * 3.0 / 4.0) - buttonHeight / 2.0;
        button = new UIButton(UIButtonType.RoundedRect);
        button.setFrame(new CGRect(buttonX, buttonY, buttonWidth, buttonHeight));
        button.setTitle("Click me!", UIControlState.Normal);
        button.getTitleLabel().setFont(UIFont.getBoldSystemFont(40));

        // Listen on clicks on the button and update the label.
        button.addOnPrimaryActionTriggeredListener((control) -> label.setText("Click Nr. " + (++clickCount)));
        view.addSubview(button);
    }
}
