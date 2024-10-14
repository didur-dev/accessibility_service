import 'accessibility_service_event.dart';
import 'accessibility_service_node.dart';

class AccessibilityServiceResult {
  String? text;
  AccessibilityServiceEvent? event;
  Map<String, AccessibilityServiceNode>? nodes;

  AccessibilityServiceResult({
    this.text,
    this.event,
    this.nodes,
  });

  AccessibilityServiceResult copyWith({
    String? text,
    AccessibilityServiceEvent? event,
    Map<String, AccessibilityServiceNode>? nodes,
  }) {
    return AccessibilityServiceResult(
      text: text ?? this.text,
      event: event ?? this.event,
      nodes: nodes ?? this.nodes,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'text': text,
      'event': event,
      'nodes': nodes,
    };
  }

  factory AccessibilityServiceResult.fromJson(Map<String, dynamic> json) {
    return AccessibilityServiceResult(
      text: json['text'] == null ? null : json['text'],
      event: json['event'] == null ? null : AccessibilityServiceEvent.fromJson(Map.from(json['event'])),
      nodes: (json['nodes'] as Map<dynamic, dynamic>?)
          ?.map((k, e) => MapEntry(k!, AccessibilityServiceNode.fromJson(Map.from(e)))),
    );
  }

  @override
  String toString() {
    String str = '\n---↓↓↓ RESULT ↓↓↓---\nEVENT:\n$event\nNODES:\n';
    str += '$text\n' ?? '';
    nodes?.forEach((treeId, e) {
      str += '$treeId : $e\n';
    });
    str += '\n---↑↑↑ RESULT ↑↑↑---\n';
    return str;
  }

  @override
  int get hashCode => Object.hash(text, event, nodes);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AccessibilityServiceResult &&
          runtimeType == other.runtimeType &&
          text == other.text &&
          event == other.event &&
          nodes == other.nodes;
}
