import 'accessibility_service_event.dart';
import 'accessibility_service_node.dart';

class AccessibilityServiceResult {
  String? text;
  String? imagePath;
  List<ClickableElement> clickableElements;
  AccessibilityServiceEvent? event;
  Map<String, AccessibilityServiceNode>? nodes;

  AccessibilityServiceResult({
    this.text,
    this.imagePath,
    this.clickableElements = const [],
    this.event,
    this.nodes,
  });

  AccessibilityServiceResult copyWith({
    String? text,
    String? imagePath,
    List<ClickableElement>? clickableElements,
    AccessibilityServiceEvent? event,
    Map<String, AccessibilityServiceNode>? nodes,
  }) {
    return AccessibilityServiceResult(
      text: text ?? this.text,
      imagePath: imagePath ?? this.imagePath,
      clickableElements: clickableElements ?? this.clickableElements,
      event: event ?? this.event,
      nodes: nodes ?? this.nodes,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'text': text,
      'imagePath': imagePath,
      'clickableElements': clickableElements,
      'event': event,
      'nodes': nodes,
    };
  }

  factory AccessibilityServiceResult.fromJson(Map<String, dynamic> json) {
    return AccessibilityServiceResult(
      text: json['text'],
      imagePath: json['imagePath'],
      clickableElements: json['clickableElements'] == null
          ? []
          : List<ClickableElement>.from((json['clickableElements'] as List).map((e) => ClickableElement.fromJson(e))),
      event: json['event'] == null ? null : AccessibilityServiceEvent.fromJson(Map.from(json['event'])),
      nodes: (json['nodes'] as Map<dynamic, dynamic>?)
          ?.map((k, e) => MapEntry(k!, AccessibilityServiceNode.fromJson(Map.from(e)))),
    );
  }

  @override
  String toString() {
    String str = '\n---↓↓↓ RESULT ↓↓↓---\nEVENT:\n$event\nNODES:\n';
    str += '$text\n' ?? '';
    str += '$imagePath\n' ?? '';
    nodes?.forEach((treeId, e) {
      str += '$treeId : $e\n';
    });
    str += '\n---↑↑↑ RESULT ↑↑↑---\n';
    return str;
  }

  @override
  int get hashCode => Object.hash(text, imagePath, event, nodes);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AccessibilityServiceResult &&
          runtimeType == other.runtimeType &&
          text == other.text &&
          imagePath == other.imagePath &&
          event == other.event &&
          nodes == other.nodes;
}

class ClickableElement {
  final String id;
  final String? text;
  List<int>? bounds;

  ClickableElement({
    required this.id,
    this.text,
    required this.bounds,
  });

  factory ClickableElement.fromJson(Map<String, dynamic> json) {
    return ClickableElement(
      id: json['id'],
      text: json['text'],
      bounds: (json['bounds'] as Map<String, dynamic>?)?.values.map((value) => value as int).toList(),
    );
  }

  @override
  String toString() {
    return 'ClickableElement{id: $id, text: $text, bounds: $bounds}';
  }
}
