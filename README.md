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
