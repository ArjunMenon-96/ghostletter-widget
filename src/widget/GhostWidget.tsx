import React from "react";
import {
  FlexWidget,
  ImageWidget,
  ListWidget,
  TextWidget,
} from "react-native-android-widget";

export type WidgetNewsItem = {
  id: string;
  title: string;
  source: string;
  sourceColor: string;
  timeAgo: string;
  category: string;
};

type Props = {
  items: WidgetNewsItem[];
  lastUpdated?: string;
};

function SourceDot({ color }: { color: string }) {
  return (
    <FlexWidget
      style={{
        width: 6,
        height: 6,
        borderRadius: 3,
        backgroundColor: color,
      }}
    />
  );
}

function HeadlineRow({ item }: { item: WidgetNewsItem }) {
  return (
    <FlexWidget
      style={{
        flexDirection: "column",
        paddingVertical: 6,
        paddingHorizontal: 10,
        borderBottomWidth: 1,
        borderBottomColor: "#27272a",
      }}
    >
      {/* Source + time row */}
      <FlexWidget
        style={{
          flexDirection: "row",
          alignItems: "center",
          marginBottom: 3,
          gap: 4,
        }}
      >
        <SourceDot color={item.sourceColor} />
        <TextWidget
          text={item.source}
          style={{
            fontSize: 9,
            color: item.sourceColor,
            fontWeight: "bold",
          }}
        />
        <TextWidget
          text={`  ·  ${item.timeAgo}`}
          style={{
            fontSize: 9,
            color: "#52525b",
          }}
        />
      </FlexWidget>

      {/* Headline */}
      <TextWidget
        text={item.title}
        style={{
          fontSize: 11,
          color: "#e4e4e7",
          fontWeight: "600",
        }}
        maxLines={2}
      />
    </FlexWidget>
  );
}

export function GhostWidget({ items, lastUpdated }: Props) {
  const displayItems = items.slice(0, 4);

  return (
    <FlexWidget
      style={{
        height: "match_parent",
        width: "match_parent",
        flexDirection: "column",
        backgroundColor: "#09090b",
        borderRadius: 16,
        overflow: "hidden",
      }}
    >
      {/* Widget header */}
      <FlexWidget
        style={{
          flexDirection: "row",
          alignItems: "center",
          paddingHorizontal: 10,
          paddingVertical: 8,
          backgroundColor: "#18181b",
          borderBottomWidth: 1,
          borderBottomColor: "#27272a",
        }}
      >
        <FlexWidget
          style={{
            width: 20,
            height: 20,
            borderRadius: 6,
            backgroundColor: "#7c3aed",
            alignItems: "center",
            justifyContent: "center",
            marginRight: 7,
          }}
        >
          <TextWidget
            text="₿"
            style={{ color: "#fff", fontSize: 11, fontWeight: "700" }}
          />
        </FlexWidget>

        <TextWidget
          text="GhostLetter"
          style={{
            color: "#f4f4f5",
            fontSize: 12,
            fontWeight: "700",
            flex: 1,
          }}
        />

        <FlexWidget
          style={{
            flexDirection: "row",
            alignItems: "center",
            gap: 4,
          }}
        >
          <FlexWidget
            style={{
              width: 5,
              height: 5,
              borderRadius: 3,
              backgroundColor: "#a78bfa",
            }}
          />
          <TextWidget
            text="LIVE"
            style={{
              color: "#a78bfa",
              fontSize: 8,
              fontWeight: "700",
              letterSpacing: 1,
            }}
          />
        </FlexWidget>
      </FlexWidget>

      {/* Headlines */}
      {displayItems.length > 0 ? (
        <ListWidget
          style={{ flex: 1 }}
        >
          {displayItems.map((item) => (
            <HeadlineRow key={item.id} item={item} />
          ))}
        </ListWidget>
      ) : (
        <FlexWidget
          style={{ flex: 1, alignItems: "center", justifyContent: "center" }}
        >
          <TextWidget
            text="Loading news..."
            style={{ color: "#52525b", fontSize: 12 }}
          />
        </FlexWidget>
      )}

      {/* Footer */}
      {lastUpdated && (
        <FlexWidget
          style={{
            paddingHorizontal: 10,
            paddingVertical: 4,
            backgroundColor: "#09090b",
            borderTopWidth: 1,
            borderTopColor: "#27272a",
          }}
        >
          <TextWidget
            text={`Updated ${lastUpdated}`}
            style={{ color: "#3f3f46", fontSize: 8 }}
          />
        </FlexWidget>
      )}
    </FlexWidget>
  );
}
