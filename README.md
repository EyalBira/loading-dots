#LoadingDots for Android

Customizable bouncing dots view for smooth loading effect. Mostly used in chat bubbles to indicate the other person is typing.

##Features

 - LoadingDots animated view
 - Use in xml
 - Customize dots appearance
 - Customize animation behavior
 - Customize animation duration

##Demo

![](screens/demo.gif)

##Usage

For basic usage, simply add to layout xml:

 ```xml
    <com.eyalbira.loadingdots.LoadingDots
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>
 ```

To customize, simply use the view attributes:

 ```xml
    <com.eyalbira.loadingdots.LoadingDots
            xmlns:app="http://schemas.android.com/apk/res-auto"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:LoadingDots_dots_color="@android:color/holo_blue_light"
            app:LoadingDots_dots_size="3dp"/>
 ```
