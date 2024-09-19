class AccessibilityServiceEvent {
  String? type;
  String? packageName;
  String? className;
  String? text;
  String? description;

  AccessibilityServiceEvent({
    this.type,
    this.packageName,
    this.className,
    this.text,
    this.description,
  });

  AccessibilityServiceEvent copyWith({
    String? type,
    String? packageName,
    String? className,
    String? text,
    String? description,
  }) {
    return AccessibilityServiceEvent(
      type: type ?? this.type,
      packageName: packageName ?? this.packageName,
      className: className ?? this.className,
      text: text ?? this.text,
      description: description ?? this.description,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'packageName': packageName,
      'className': className,
      'text': text,
      'description': description,
    };
  }

  factory AccessibilityServiceEvent.fromJson(Map<String, dynamic> json) {
    return AccessibilityServiceEvent(
      type: json['type'] as String?,
      packageName: json['packageName'] as String?,
      className: json['className'] as String?,
      text: json['text'] as String?,
      description: json['description'] as String?,
    );
  }

  @override
  String toString() => "($type) $packageName / $className { '$text' | $description }";

  @override
  int get hashCode => Object.hash(type, packageName, className, text, description);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AccessibilityServiceEvent &&
          runtimeType == other.runtimeType &&
          type == other.type &&
          packageName == other.packageName &&
          className == other.className &&
          text == other.text &&
          description == other.description;
}
