import React from "react";
import {
  Image,
  Linking,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { formatDistanceToNow } from "date-fns";
import { NewsItem } from "../lib/api";

type Props = {
  item: NewsItem;
  index: number;
  onPress?: () => void;
};

const CAT_ICONS: Record<string, string> = {
  DeFi: "◈",
  Markets: "▲",
  Regulation: "⚖",
  NFT: "◉",
  Blockchain: "⬡",
  News: "◎",
  Mining: "⛏",
  Fintech: "₿",
};

export default function NewsCard({ item, index, onPress }: Props) {
  const timeAgo = (() => {
    try {
      return formatDistanceToNow(new Date(item.pubDate), { addSuffix: true });
    } catch {
      return "recently";
    }
  })();

  function handlePress() {
    if (onPress) onPress();
    else Linking.openURL(item.link);
  }

  const icon = CAT_ICONS[item.category] ?? "◎";

  return (
    <TouchableOpacity
      style={styles.row}
      onPress={handlePress}
      activeOpacity={0.7}
    >
      {/* Thumbnail */}
      <View style={styles.thumbWrap}>
        {item.imageUrl ? (
          <Image
            source={{ uri: item.imageUrl }}
            style={styles.thumb}
            resizeMode="cover"
          />
        ) : (
          <View style={[styles.thumbFallback, { borderLeftColor: item.sourceColor }]}>
            <Text style={[styles.fallbackIcon, { color: item.sourceColor }]}>{icon}</Text>
            <Text style={styles.fallbackSource} numberOfLines={1}>
              {item.source.split(" ")[0].toUpperCase()}
            </Text>
          </View>
        )}
        {/* Index badge */}
        <View style={styles.indexBadge}>
          <Text style={styles.indexText}>{String(index + 1).padStart(2, "0")}</Text>
        </View>
      </View>

      {/* Text block */}
      <View style={styles.textBlock}>
        <View style={styles.metaRow}>
          <Text style={styles.sourceLabel}>{item.source.toUpperCase()}</Text>
          <Text style={styles.catLabel}>{item.category.toUpperCase()}</Text>
          <Text style={styles.timeLabel}>{timeAgo}</Text>
        </View>
        <Text style={styles.title} numberOfLines={2}>{item.title}</Text>
        {item.summary ? (
          <Text style={styles.summary} numberOfLines={1}>{item.summary}</Text>
        ) : null}
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    borderBottomWidth: 1,
    borderBottomColor: "#1a1a14",
    backgroundColor: "#0a0a08",
  },

  /* Thumbnail */
  thumbWrap: {
    width: 84,
    height: 80,
    flexShrink: 0,
    position: "relative",
    borderRightWidth: 1,
    borderRightColor: "#1a1a14",
    backgroundColor: "#111109",
  },
  thumb: {
    width: "100%",
    height: "100%",
  },
  thumbFallback: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    borderLeftWidth: 2,
    borderLeftColor: "#f5a623",
  },
  fallbackIcon: {
    fontSize: 22,
    opacity: 0.6,
  },
  fallbackSource: {
    fontSize: 6,
    color: "#444",
    marginTop: 3,
    letterSpacing: 0.5,
    fontWeight: "700",
  },
  indexBadge: {
    position: "absolute",
    top: 4,
    left: 4,
    backgroundColor: "rgba(0,0,0,0.75)",
    paddingHorizontal: 4,
    paddingVertical: 1,
  },
  indexText: {
    color: "#f5a623",
    fontSize: 8,
    fontWeight: "700",
    letterSpacing: 0.5,
  },

  /* Text */
  textBlock: {
    flex: 1,
    paddingHorizontal: 12,
    paddingVertical: 10,
    justifyContent: "center",
  },
  metaRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginBottom: 5,
    flexWrap: "wrap",
  },
  sourceLabel: {
    fontSize: 8,
    fontWeight: "700",
    color: "#a89060",
    backgroundColor: "#1e1e14",
    paddingHorizontal: 5,
    paddingVertical: 1,
    letterSpacing: 0.3,
  },
  catLabel: {
    fontSize: 8,
    fontWeight: "700",
    color: "#52c41a",
    letterSpacing: 0.3,
  },
  timeLabel: {
    fontSize: 8,
    color: "#3a3a30",
    marginLeft: "auto",
  },
  title: {
    color: "#d4c08a",
    fontSize: 13,
    fontWeight: "600",
    lineHeight: 18,
    marginBottom: 3,
  },
  summary: {
    color: "#4a4a40",
    fontSize: 11,
    lineHeight: 15,
  },
});
