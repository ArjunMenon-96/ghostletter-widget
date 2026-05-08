import React from "react";
import {
  FlexWidget,
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

function StoryRow({ item, index }: { item: WidgetNewsItem; index: number }) {
  return (
    <FlexWidget
      style={{
        flexDirection: "row",
        paddingVertical: 7,
        paddingHorizontal: 10,
        borderBottomWidth: 1,
        borderBottomColor: "#1a1a14",
        backgroundColor: "#0a0a08",
      }}
    >
      {/* Index */}
      <TextWidget
        text={String(index + 1).padStart(2, "0")}
        style={{
          fontSize: 9,
          color: "#f5a623",
          fontWeight: "700",
          marginRight: 8,
          marginTop: 1,
        }}
      />

      {/* Content */}
      <FlexWidget style={{ flexDirection: "column", width: "match_parent" }}>
        {/* Meta row */}
        <FlexWidget
          style={{ flexDirection: "row", alignItems: "center", marginBottom: 3 }}
        >
          <TextWidget
            text={item.source.toUpperCase()}
            style={{ fontSize: 8, color: "#a89060", fontWeight: "700", marginRight: 6 }}
          />
          <TextWidget
            text={item.category.toUpperCase()}
            style={{ fontSize: 8, color: "#52c41a", fontWeight: "700", marginRight: 6 }}
          />
          <TextWidget
            text={item.timeAgo}
            style={{ fontSize: 8, color: "#3a3a30" }}
          />
        </FlexWidget>

        {/* Headline */}
        <TextWidget
          text={item.title}
          style={{ fontSize: 11, color: "#d4c08a", fontWeight: "600" }}
          maxLines={2}
        />
      </FlexWidget>
    </FlexWidget>
  );
}

export function GhostWidget({ items, lastUpdated }: Props) {
  const displayItems = items.slice(0, 5);

  return (
    <FlexWidget
      style={{
        height: "match_parent",
        width: "match_parent",
        flexDirection: "column",
        backgroundColor: "#0a0a08",
        borderRadius: 14,
        overflow: "hidden",
      }}
    >
      {/* Header */}
      <FlexWidget
        style={{
          flexDirection: "row",
          alignItems: "center",
          paddingHorizontal: 10,
          paddingVertical: 7,
          backgroundColor: "#111109",
          borderBottomWidth: 1,
          borderBottomColor: "#2a2a20",
        }}
      >
        <TextWidget
          text="GHOSTLETTER"
          style={{
            color: "#f5a623",
            fontSize: 11,
            fontWeight: "700",
            marginRight: 8,
          }}
        />

        <FlexWidget style={{ flexDirection: "row", alignItems: "center" }}>
          <FlexWidget
            style={{ width: 5, height: 5, borderRadius: 3, backgroundColor: "#f5a623", marginRight: 4 }}
          />
          <TextWidget
            text="LIVE"
            style={{ color: "#f5a623", fontSize: 8, fontWeight: "700" }}
          />
        </FlexWidget>
      </FlexWidget>

      {/* Stories */}
      {displayItems.length > 0 ? (
        <ListWidget style={{ height: "match_parent" }}>
          {displayItems.map((item, i) => (
            <StoryRow key={item.id} item={item} index={i} />
          ))}
        </ListWidget>
      ) : (
        <FlexWidget style={{ height: "match_parent", alignItems: "center", justifyContent: "center" }}>
          <TextWidget
            text="FETCHING FEED..."
            style={{ color: "#3a3a30", fontSize: 10, fontWeight: "700" }}
          />
        </FlexWidget>
      )}

      {/* Footer */}
      <FlexWidget
        style={{
          flexDirection: "row",
          alignItems: "center",
          paddingHorizontal: 10,
          paddingVertical: 5,
          backgroundColor: "#111109",
          borderTopWidth: 1,
          borderTopColor: "#1a1a14",
        }}
      >
        <TextWidget
          text="ghostletter111.vercel.app"
          style={{ color: "#2a2a20", fontSize: 7, marginRight: 8 }}
        />
        {lastUpdated && (
          <TextWidget
            text={`UPD ${lastUpdated}`}
            style={{ color: "#3a3a30", fontSize: 7, fontWeight: "700" }}
          />
        )}
      </FlexWidget>
    </FlexWidget>
  );
}
