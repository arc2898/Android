import 'package:ft_music/core/theme/app_theme.dart';
import 'package:ft_music/blocs/settings_cubit/cubit/settings_cubit.dart';
import 'package:ft_music/l10n/app_localizations.dart';
import 'package:ft_music/screens/screen/search_screen.dart';
import 'package:ft_music/screens/screen/home_views/setting_views/setting_shared_widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

/// Lightweight discovery preferences. Categories are local suggestions and do
/// not fetch anything until the user taps one, keeping startup and data use low.
class AppUISettings extends StatelessWidget {
  const AppUISettings({super.key});

  static const _groups = <_CategoryGroup>[
    _CategoryGroup(
      title: 'Hits',
      icon: Icons.whatshot_rounded,
      categories: [
        'Global Top Hits',
        'Viral Now',
        'New Music Friday',
        'Top 50 Worldwide',
      ],
    ),
    _CategoryGroup(
      title: 'English',
      icon: Icons.language_rounded,
      categories: [
        'English Pop Hits',
        'English Hip-Hop',
        'English R&B',
        'Indie Alternative',
      ],
    ),
    _CategoryGroup(
      title: 'Regional • North',
      icon: Icons.north_rounded,
      categories: [
        'Hindi Hits',
        'Punjabi Hits',
        'Bengali Hits',
      ],
    ),
    _CategoryGroup(
      title: 'Regional • South',
      icon: Icons.south_rounded,
      categories: [
        'Tamil Hits',
        'Telugu Hits',
        'Malayalam Hits',
        'Kannada Hits',
      ],
    ),
    _CategoryGroup(
      title: 'Phonk',
      icon: Icons.bolt_rounded,
      categories: [
        'Drift Phonk',
        'Brazilian Phonk',
        'Aggressive Phonk',
        'Chill Phonk',
      ],
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      backgroundColor: Default_Theme.themeColor,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'UI & Services',
          style: const TextStyle(
            color: Default_Theme.primaryColor1,
            fontSize: 22,
            fontWeight: FontWeight.w800,
          ).merge(Default_Theme.secondoryTextStyleMedium),
        ),
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final horizontalPadding = constraints.maxWidth >= 700 ? 32.0 : 18.0;
          return ListView(
            physics: const BouncingScrollPhysics(),
            padding: EdgeInsets.fromLTRB(
                horizontalPadding, 8, horizontalPadding, 36),
            children: [
              SettingSectionHeader(label: 'Interface & services'),
              SettingCard(
                children: [
                  SettingToggleTile(
                    icon: Icons.view_carousel_rounded,
                    title: 'Auto-slide charts',
                    subtitle: 'Automatically rotate charts on Discover.',
                    value: context.watch<SettingsCubit>().state.autoSlideCharts,
                    onChanged: (value) =>
                        context.read<SettingsCubit>().setAutoSlideCharts(value),
                  ),
                  const SettingDivider(),
                  SettingToggleTile(
                    icon: Icons.radio_rounded,
                    title: 'Last.FM picks',
                    subtitle: 'Show personalised Last.FM recommendations.',
                    value: context.watch<SettingsCubit>().state.lFMPicks,
                    onChanged: (value) =>
                        context.read<SettingsCubit>().setLastFMExpore(value),
                  ),
                  const SettingDivider(),
                  SettingToggleTile(
                    icon: Icons.sync_rounded,
                    title: 'Last.FM scrobbling',
                    subtitle: 'Send played tracks to Last.FM when enabled.',
                    value: context.watch<SettingsCubit>().state.lastFMScrobble,
                    onChanged: (value) =>
                        context.read<SettingsCubit>().setLastFMScrobble(value),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              Text(
                'Pick a lane. FT-music will search it only when you ask.',
                style: const TextStyle(
                  color: Default_Theme.primaryColor2,
                  fontSize: 14,
                  height: 1.35,
                ).merge(Default_Theme.secondoryTextStyle),
              ),
              const SizedBox(height: 22),
              for (final group in _groups) ...[
                _CategoryGroupCard(group: group),
                const SizedBox(height: 16),
              ],
              SettingSectionHeader(label: l10n.settingsPluginDefaults),
              const SettingCard(
                children: [
                  Padding(
                    padding: EdgeInsets.all(16),
                    child: Row(
                      children: [
                        Icon(Icons.speed_rounded,
                            color: Default_Theme.accentColor2),
                        SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            'Spotify is preferred when a compatible Spotify plugin is installed. Otherwise FT-music keeps the fastest available resolver.',
                            style: TextStyle(
                              color: Default_Theme.primaryColor2,
                              fontSize: 13,
                              height: 1.35,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ],
          );
        },
      ),
    );
  }
}

class _CategoryGroup {
  final String title;
  final IconData icon;
  final List<String> categories;

  const _CategoryGroup({
    required this.title,
    required this.icon,
    required this.categories,
  });
}

class _CategoryGroupCard extends StatelessWidget {
  final _CategoryGroup group;
  const _CategoryGroupCard({required this.group});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
      decoration: BoxDecoration(
        color: Default_Theme.surfaceColor,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(
          color: Default_Theme.accentColor2.withValues(alpha: .12),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(group.icon, color: Default_Theme.accentColor2, size: 22),
              const SizedBox(width: 10),
              Text(
                group.title,
                style: const TextStyle(
                  color: Default_Theme.primaryColor1,
                  fontSize: 17,
                  fontWeight: FontWeight.w800,
                ).merge(Default_Theme.secondoryTextStyleMedium),
              ),
            ],
          ),
          const SizedBox(height: 12),
          LayoutBuilder(
            builder: (context, constraints) {
              final columns = constraints.maxWidth >= 480 ? 3 : 2;
              final width =
                  (constraints.maxWidth - (columns - 1) * 8) / columns;
              return Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final category in group.categories)
                    SizedBox(
                      width: width,
                      child: OutlinedButton(
                        onPressed: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => SearchScreen(searchQuery: category),
                          ),
                        ),
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 12),
                          foregroundColor: Default_Theme.primaryColor1,
                          side: BorderSide(
                            color: Default_Theme.primaryColor2
                                .withValues(alpha: .18),
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                        ),
                        child: Text(
                          category,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.center,
                          style: const TextStyle(fontSize: 12),
                        ),
                      ),
                    ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}
