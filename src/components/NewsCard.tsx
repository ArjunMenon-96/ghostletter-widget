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
  onPress?: () => void;
};

export default function NewsCard({ item, onPress }: Props) {
  const timeAgo = (() => {
    try {
      return formatDistanceToNow(new Date(item.pubDate), { addSuffix: true });
    } catch {
      return "recently";
    }
  })();

  function handlePress() {
    if (onPress) {
      onPress();
    } else {
      Linking.openURL(item.link);
    }
  }

  return (
    <TouchableOpacity
      style={styles.card}
      onPress={handlePress}
      activeOpacity={0.75}
    >
      <View style={styles.body}>
        <View style={styles.meta}>
          <View
            style={[
              styles.sourceBadge,
              { backgroundColor: item.sourceColor + "22", borderColor: item.sourceColor + "44" },
            ]}
          >
            <View style={[styles.dot, { backgroundColor: item.sourceColor }]} />
            <Text style={[styles.sourceText, { color: item.sourceColor }]}>
              {item.source}
            </Text>
          </View>
          <View style={styles.categoryBadge}>
            <Text style={styles.categoryText}>{item.category}</Text>
          </View>
          <Text style={styles.time}>{timeAgo}</Text>
        </View>

        <View style={styles.content}>
          {item.imageUrl ? (
            <Image
              source={{ uri: item.imageUrl }}
              style={styles.thumbnail}
              resizeMode="cover"
            />
          ) : null}
          <View style={styles.textBlock}>
            <Text style={styles.title} numberOfLines={2}>
              {item.title}
            </Text>
            {item.summary ? (
              <Text style={styles.summary} numberOfLines={2}>
                {item.summary}
              </Text>
            ) : null}
          </View>
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: "#18181b",
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "#27272a",
    marginBottom: 10,
    overflow: "hidden",
  },
  body: {
    padding: 14,
  },
  meta: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginBottom: 10,
    flexWrap: "wrap",
  },
  sourceBadge: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 20,
    borderWidth: 1,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 5,
  },
  sourceText: {
    fontSize: 11,
    fontWeight: "600",
  },
  categoryBadge: {
    backgroundColor: "#27272a",
    borderRadius: 20,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  categoryText: {
    color: "#a1a1aa",
    fontSize: 11,
  },
  time: {
    color: "#52525b",
    fontSize: 11,
    marginLeft: "auto",
  },
  content: {
    flexDirection: "row",
    gap: 10,
  },
  thumbnail: {
    width: 70,
    height: 70,
    borderRadius: 8,
    backgroundColor: "#27272a",
    flexShrink: 0,
  },
  textBlock: {
    flex: 1,
  },
  title: {
    color: "#f4f4f5",
    fontSize: 14,
    fontWeight: "600",
    lineHeight: 20,
    marginBottom: 4,
  },
  summary: {
    color: "#71717a",
    fontSize: 12,
    lineHeight: 17,
  },
});
