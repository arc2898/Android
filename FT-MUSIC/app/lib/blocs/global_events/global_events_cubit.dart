import 'package:ft_music/services/db/dao/settings_dao.dart';
import 'package:bloc/bloc.dart';
import 'package:equatable/equatable.dart';

part 'global_events_state.dart';

class GlobalEventsCubit extends Cubit<GlobalEventsState> {
  final SettingsDAO _settingsDao;

  GlobalEventsCubit({required SettingsDAO settingsDao})
      : _settingsDao = settingsDao,
        super(GlobalEventsInitial());

  /// Updates are intentionally disabled in FT-music. Releases are distributed
  /// manually so this app never contacts an upstream release server.
  void checkForUpdates() {}

  void showAlertDialog(String title, String content) {
    emit(AlertDialogState(title: title, content: content));
  }
}
