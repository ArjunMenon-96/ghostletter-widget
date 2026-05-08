import { registerWidgetTaskHandler } from 'react-native-android-widget';
import { widgetTaskHandler } from './src/widget/widgetTask';

// Must be registered before registerRootComponent so it runs in headless mode
// (when Android requests a widget update without the app being open)
registerWidgetTaskHandler(widgetTaskHandler);

import { registerRootComponent } from 'expo';
import App from './App';

registerRootComponent(App);
