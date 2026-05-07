import { WidgetTaskHandlerProps } from "react-native-android-widget";
import { fetchNews } from "../lib/api";
import { GhostWidget, WidgetNewsItem } from "./GhostWidget";
import { formatDistanceToNow } from "date-fns";
import React from "react";

const nameToWidget = {
  GhostWidget,
};

export async function widgetTaskHandler(props: WidgetTaskHandlerProps) {
  const widgetInfo = props.widgetInfo;
  const Widget =
    nameToWidget[widgetInfo.widgetName as keyof typeof nameToWidget];

  switch (props.widgetAction) {
    case "WIDGET_ADDED":
    case "WIDGET_UPDATE":
    case "WIDGET_RESIZED": {
      const items = await loadWidgetData();
      props.renderWidget(
        React.createElement(Widget, {
          items,
          lastUpdated: new Date().toLocaleTimeString(),
        })
      );
      break;
    }

    case "WIDGET_DELETED":
      break;

    case "WIDGET_CLICK": {
      const items = await loadWidgetData();
      props.renderWidget(
        React.createElement(Widget, {
          items,
          lastUpdated: new Date().toLocaleTimeString(),
        })
      );
      break;
    }

    default:
      break;
  }
}

async function loadWidgetData(): Promise<WidgetNewsItem[]> {
  try {
    const news = await fetchNews("All", "");
    return news.slice(0, 4).map((item, i) => ({
      id: item.id || String(i),
      title: item.title,
      source: item.source,
      sourceColor: item.sourceColor,
      category: item.category,
      timeAgo: (() => {
        try {
          return formatDistanceToNow(new Date(item.pubDate), {
            addSuffix: true,
          });
        } catch {
          return "recently";
        }
      })(),
    }));
  } catch {
    return [];
  }
}
