// ignore_for_file: public_member_api_docs, sort_constructors_first

import 'package:ft_music/core/theme/app_theme.dart';
import 'package:flutter/material.dart';

class SettingTile extends StatelessWidget {
  final String title;
  final String subtitle;
  final Function? onTap;
  final Widget? trailing;

  const SettingTile({
    Key? key,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.trailing,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final enabled = onTap != null;
    return AnimatedContainer(
      duration: AppTheme.motionFast,
      curve: AppTheme.motionCurve,
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: enabled ? Default_Theme.surfaceColor : Colors.transparent,
        borderRadius: BorderRadius.circular(AppTheme.cornerMedium),
        border: Border.all(
          color: enabled
              ? Default_Theme.surfaceHighlight.withValues(alpha: .55)
              : Colors.transparent,
        ),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(AppTheme.cornerMedium),
          onTap: enabled ? () => onTap?.call() : null,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 12, 12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: Default_Theme.primaryColor1,
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ).merge(Default_Theme.secondoryTextStyleMedium),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        subtitle,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: Default_Theme.primaryColor2
                              .withValues(alpha: .72),
                          fontSize: 12,
                          height: 1.25,
                        ).merge(Default_Theme.secondoryTextStyle),
                      ),
                    ],
                  ),
                ),
                if (trailing != null) ...[
                  const SizedBox(width: 12),
                  trailing!,
                ] else if (enabled)
                  const Padding(
                    padding: EdgeInsets.only(left: 8),
                    child: Icon(
                      Icons.chevron_right_rounded,
                      color: Default_Theme.primaryColor2,
                      size: 22,
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
