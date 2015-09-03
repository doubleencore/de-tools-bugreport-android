#Bug Reporter

A simple library to collect the database, cache, files and basic device information from an application.

## Getting started

In your `build.gradle`:

```gradle
 dependencies {
    debugCompile project(':data-collection-library')
    releaseCompile project(':data-collection-library-no-op')
 }
```

In your `Application` class:

```java
public class ExampleApplication extends Application {

  @Override public void onCreate() {
    super.onCreate();
    DataCollection.setup(this);
  }
}
```

If you want to trigger a report from a screenshot, in your `Activity` or `Fragment` class:
```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        DataCollection.enableObserver();
    }

    @Override
    protected void onPause() {
        DataCollection.disableObserver();
        super.onPause();
    }
}
```

## Permissions

The library which should be included with debug builds requires `WRITE_EXTERNAL_STORAGE` and `READ_EXTERNAL_STORAGE`.  The no-op version does not have any permissions requirements. 

## Generating a report

To manually trigger a report:

```java
    DataCollection.executeCollection();
```

Or if you have enabled screenshot monitoring, simply trigger a screenshot on the device.

Both will result in a notification displaying which will allow you send a .zip file of the contents.
