# VLC for Android 作業ガイド

## 適用範囲と情報源

このファイルは、このリポジトリで作業するエージェント向けのガイドです。変更対象とその呼び出し元、テスト、該当するビルド設定を読んでから編集してください。

- このチェックアウトは `StuckedJp/vlc-android` の個人用フォークです。固有仕様は `README.forked.md`、今後の要望は `whatimake.md` を参照し、実装済みかどうかはコードで確認してください。要望一覧にあるだけの機能削除は実施しないでください。
- モジュール一覧と Android SDK 設定は `settings.gradle`、依存バージョンと JVM ターゲットはルートの `build.gradle` を参照してください。
- ネイティブビルドの実際の手順は `buildsystem/compile.sh` と、そこから呼ばれるスクリプトを基準にしてください。
- `README.md` の Bintray、JDK 8、旧ディレクトリ構成、ABI の `all` に関する記述は、現行設定と一致しない場合があります。
- このガイドの調査対象は Git 管理下のソース、設定、リソース、テストです。別途取得する外部リポジトリや生成物の全実装を説明するものではありません。

## このフォークで維持する仕様

- UPnP 経由の MPEG2-TS シークはネイティブ VLC 側の変更を含みます。Android 側だけで完結する修正とみなさず、ローカル LibVLC を組み込んだ `Dev` で確認してください。
- リモコンの左右キーによる通常の動画シークは ±15 秒です。入口は `gui/video/VideoPlayerActivity.kt` の DPAD 処理です。メニューやトラック選択時の方向キー操作も維持してください。
- 複数サービスを含む MPEG2-TS のチャンネル選択は `gui/dialogs/VideoTracksDialog.kt`、`media/ProgramSelection.kt`、`PlaybackService.kt`、`media/PlayerController.kt` が入口です。音声・映像トラックとは別のサービス ID を扱います。未対応 LibVLC では反射による互換ブリッジが空の一覧を返し、選択 UI を表示しません。
- チャンネル選択のネイティブ差分と過去の検証は `buildsystem/patches/vlc/README.program-selection.md` を参照してください。保存された VLC/JNI パッチは `compile.sh` が自動適用するものではありません。依存フォークに取り込まれている場合もあるため、現在のソースと適用済み状態を確認し、二重適用を避けてください。
- 頭文字ごとのリスト見出しは `show_headers` の既定値を `false` にしています。入口は `application/tools/src/main/java/org/videolan/tools/Settings.kt` と `application/vlc-android/res/xml/preferences_ui.xml` です。既存ユーザーの選択や並び順は維持し、動画グループ化の `video_min_group_length` と混同しないでください。
- 通常の再生再開位置は `application/tools/src/main/java/org/videolan/tools/PlaybackSessionStore.kt` で URI ごとにプロセス内だけ保持します。Activity/Service の再生成では維持し、プロセス終了後は先頭から再生します。「アプリ終了」を Activity の破棄と同一視しないでください。履歴・視聴済みメタデータや明示的な Intent の開始位置は別扱いです。
- 再開シークは `DeferredPlaybackResume.kt` と `PlayerController.kt` で初期バッファリング、シーク可否、時間情報が揃うまで遅延します。TS の PAT/PMT・PCR 検出前にシークを送らないこと、停止・明示シーク時の取り消し、古い入力イベントの除外、メディアセッションへの位置通知を維持してください。

## 構成

VLC for Android は Kotlin/Java と XML UI を中心にした Android マルチモジュールプロジェクトです。再生エンジンは LibVLC、メディアの索引管理はネイティブ medialibrary を使用します。画面では Fragment、ViewModel、LiveData/Flow、coroutines、Data Binding/View Binding が使われています。

| ディレクトリ | 役割・変更時の入口 |
| --- | --- |
| `application/app` | APK の組み立て、署名、ビルド種別。`src/main/java/org/videolan/mobile/app/VLCApplication.kt` と `AppSetupDelegate.kt` がアプリ初期化の入口。端末上の UI/DB テストも配置される |
| `application/vlc-android` | メインの Android ライブラリ。再生サービス、スマートフォン UI、ブラウザー、ViewModel、Repository、ウィジェット、車載連携 |
| `application/television` | Android TV の画面と操作。Leanback を使用し、メインライブラリと moviepedia に依存 |
| `application/resources` | 共通の文字列、画像、色、スタイル、定数、`VLCInstance`、`VLCOptions` |
| `application/tools` | 共通ユーティリティ、設定キー、ネットワーク監視、共有 coroutine scope 等 |
| `application/mediadb` | Room の `MediaDatabase`、DAO、Entity、Converter、Migration。ネイティブのメディア索引 DB とは別のアプリ用 DB |
| `application/moviepedia` | メタデータ取得、API、Repository、独立した Room DB、関連画面 |
| `application/live-plot-graph` | グラフ描画用 View |
| `application/donations` | 寄付関連のコードとリソース。組み込み方は依存設定を確認 |
| `medialibrary` | Android 向け Java API、実装、stub、テストと JNI。`jni/CMakeLists.txt` は C/C++17 の `mla` を構築 |
| `buildsystem` | ネイティブ/Web ビルド、公開設定、パッチ、CI、スクリーンショット等の自動化 |
| `libvlcjni` | ビルドスクリプトが取得する独立した Git リポジトリ。親リポジトリでは ignore 対象。内部の `vlc` も独立したソースツリー |

## 実装を探す入口

以下のパスは `application/vlc-android/src/org/videolan/vlc/` からの相対パスです。

- 再生とシステム連携: `PlaybackService.kt`。メディアセッション、通知、音声フォーカス、サービスのライフサイクルも確認してください。
- キューと再生状態: `media/PlaylistManager.kt`。エンジン操作は `media/PlayerController.kt` も確認してください。
- メディア探索・索引更新: `MediaParsingService.kt` と `providers/medialibrary/`。
- ファイル・ネットワーク閲覧: `providers/` と `viewmodels/browser/`。
- 画面: `gui/`、`gui/video/VideoPlayerActivity.kt`、`viewmodels/`。起動振り分けは `StartActivity.kt`。
- アプリ DB へのアクセス: `repository/`。DB 定義と migration は `application/mediadb/src/main/java/org/videolan/vlc/database/`。
- ストレージ権限: `util/Permissions.kt`、`gui/helpers/hf/StoragePermissionsDelegate.kt`。

medialibrary の API 変更では `medialibrary/src/`、`vlc3/src/`、`vlc4/src/`、stub と `medialibrary/jni/` を横断して調べてください。JNI の登録名、シグネチャ、Java 側の宣言は一致させる必要があります。

## ソース配置の注意

- `application/vlc-android` は通常の `src/main/java` ではなく、`src/`、`res/`、ルートの `AndroidManifest.xml` を使用します。
- 同モジュールの単体テストは `test/`、共有テストコードは `test-common/`。`androidTest/` も source set に設定されています。アプリの instrumentation テストは `application/app/src/androidTest/` も確認してください。
- `medialibrary` も `src/`、`res/`、`test/` を使います。他の多くのモジュールは `src/main/` 構成です。
- `application/vlc-android`、`application/resources`、`medialibrary` には VLC 3/4 用の `vlc3/src` と `vlc4/src` があります。`-PforceVlc4=true` の有無で選択が変わります。共通 API を変更するときは両方を確認してください。
- `application/resources/src/main/res/values-iw` と `medialibrary/res/values-iw` はシンボリックリンクです。翻訳を一括編集するときはリンク先との重複を避けてください。

## ビルド

リポジトリルートから実行します。ネイティブビルドには `ANDROID_SDK` と `ANDROID_NDK` が必要です。スクリプトは `local.properties` に SDK/NDK のパスと NDK バージョンを設定します。

2026-10-23 に確認した設定は Gradle 9.6.0、AGP 9.4.1、Kotlin 2.2.10、compile/target SDK 36、Build Tools 36.0.0 です。生成する Java/Kotlin バイトコードのターゲットは 1.8 ですが、Gradle 自体を動かす JDK とは区別してください。過去の調査では JDK 21 で Gradle の起動を確認しています。

VLC 3 の minSdk は 17、VLC 4 は 23 です。NDK は `local.properties` の指定が優先されます。ルート設定には VLC 3 用の既定 NDK 21 と API 17 互換性に関する注意があります。手元の NDK を全環境の推奨値とみなさず、対象 VLC/API/ABI を確認してください。

```sh
# 初期化（外部ソース取得、Gradle wrapper とローカル設定の準備）
./buildsystem/compile.sh --init

# 公開済みネイティブ依存を使って Android アプリを構築
./gradlew :application:app:assembleDebug

# ローカルの LibVLC と medialibrary を含むアプリの構築
./buildsystem/compile.sh -a arm64-v8a

# LibVLC のみを release AAR として構築
./buildsystem/compile.sh -l -a arm64-v8a -r

# medialibrary の構築
./buildsystem/compile.sh -ml -a arm64-v8a -r
```

- `Debug`/`Release` のアプリ依存は公開された LibVLC/medialibrary を使用し、`Dev` はローカルプロジェクトを使用します。ネイティブの修正を `assembleDebug` だけで検証できるとは限りません。
- 対応 ABI は `armeabi-v7a`、`arm64-v8a`、`x86`、`x86_64`。`arm`、`arm64` は別名です。現在の `compile.sh` は `-a all` を受け付けません。
- VLC 4 の初期化には `-vlc4`、Gradle の呼び出しには `-PforceVlc4=true` を指定します。
- `-t` は利用可能なら prebuilt contrib を使います。`-b` は VLC ソース検査の迂回であり、一般的なビルド修復オプションではありません。
- `--reset` は外部ソースで `git reset --hard` を実行する経路があります。ローカル変更を確認せず実行しないでください。
- `application/app/build/outputs/` に APK、各ライブラリの `build/outputs/aar/` に AAR が出力されます。正確な名前と ABI は各モジュールの Gradle 設定を確認してください。

## 依存フォークと固定リビジョン

取得元・ブランチ・固定コミットはビルドスクリプトを正としてください。2026-10-23 時点では次の構成です。

| 依存 | 取得先・ブランチ | 固定コミットの定義 |
| --- | --- | --- |
| LibVLC JNI（VLC 3） | `StuckedJp/libvlcjni`、`libvlcjni-3.x` | `buildsystem/compile.sh` の `LIBVLCJNI_TESTED_HASH` |
| LibVLC JNI（VLC 4） | VideoLAN upstream、`master` | 同スクリプトの VLC 4 分岐 |
| VLC | 個人用依存フォークは `StuckedJp/vlc` | 取得した `libvlcjni` 内の取得スクリプトで確認 |
| medialibrary | `StuckedJp/medialibrary`、`0.13.x` | `buildsystem/compile-medialibrary.sh` の `MEDIALIBRARY_HASH` |
| libvlcpp | `StuckedJp/libvlcpp`、`master` を取得後に互換コミットへ切替 | 同スクリプトの `LIBVLCPP_HASH` |

- 個人用フォークの取得は GitHub SSH URL を使用します。取得環境に SSH 認証が必要です。
- libvlcpp の fork の `master` は VLC 4 系です。VLC 3 用の固定コミットと、その後に適用する `buildsystem/patches/libvlcpp/` を維持してください。
- 固定値を書き換えても既存 checkout が自動的に切り替わるとは限りません。取得処理の条件と各リポジトリの HEAD・ローカル変更を確認してください。更新のためだけに `--reset` を実行しないでください。
- 外部ソースは `libvlcjni/`、`libvlcjni/vlc/`、`medialibrary/medialibrary/`、`medialibrary/medialibrary/libvlcpp/` にあります。ネイティブ変更ではそれぞれの状態と差分を確認してください。

## 検証

変更範囲に対応する最小限のテストから実行してください。以下は設定に基づくコマンド例であり、この文書の作成時に全実行したものではありません。

```sh
# プロセス内の再生位置保持と遅延再開の単体テスト
./gradlew :application:tools:testDebugUnitTest --tests 'org.videolan.tools.PlaybackSessionStoreTest' --tests 'org.videolan.tools.DeferredPlaybackResumeTest'

# メインライブラリの単体テスト
./gradlew :application:vlc-android:testDebugUnitTest

# 特定のテストクラスだけ実行
./gradlew :application:vlc-android:testDebugUnitTest --tests 'org.videolan.vlc.util.StringsTest'

# medialibrary の単体テスト
./gradlew :medialibrary:testDebugUnitTest

# 実機またはエミュレーターを使用するアプリのテスト
./gradlew :application:app:connectedDebugAndroidTest

# Android lint
./gradlew :application:app:lintDebug
```

- JUnit、Robolectric、MockK/Mockito、coroutines のテスト補助が使われています。`application/vlc-android/test/org/videolan/vlc/BaseTest.kt` と近隣のテストを参考にしてください。
- DB 変更では DAO と migration のテストを確認してください。既存テストには `application/app/src/androidTest/java/org/videolan/vlc/database/` があります。
- instrumentation テストは Orchestrator と `clearPackageData` を使用します。テスト実行によるアプリデータの初期化を考慮してください。
- メインライブラリの単体テスト設定には `-noverify` が残っています。JDK やテストランナーの互換性エラーは、アプリの機能不良と区別して報告してください。
- `application/app` の lint は `abortOnError = false` です。終了コードだけでなくレポートも確認してください。
- CI の定義は `buildsystem/gitlab/.gitlab-ci.yml`。`buildsystem/check-all-commits.sh` は rebase の継続等も扱う専用スクリプトで、通常の単体テスト用コマンドではありません。
- UI/再生の変更では、対象端末でバックグラウンド移行、権限、画面回転、音声/動画切替、TV 操作など変更に関係する経路を確認し、実行した検証と未確認事項を報告してください。

## 編集方針

- 周囲の Kotlin/Java/XML/C++ の書式と既存の抽象化に合わせ、無関係な整形や依存更新を混ぜないでください。ライセンス・著作権表記を維持してください。
- I/O、DB、ネットワーク処理は適切な dispatcher で行い、既存の lifecycle scope とキャンセル処理を維持してください。再生状態の通知順、コールバック解除、サービス終了処理も確認してください。
- LibVLC オブジェクトの `retain`/`release`、JNI の参照と配列の解放を保ち、所有権を確認してください。
- DB の Entity/DAO を変更するときは、バージョン、migration、既存ユーザーデータの互換性を併せて扱ってください。
- ユーザー向け文字列は該当モジュールのリソースへ追加してください。共有文字列は `application/resources` を確認し、プレースホルダーと複数形を維持してください。翻訳は Transifex 経由の運用です。
- UI 変更は共通画面、スマートフォン、TV のどこに属するかを確認してください。共通 API の変更では TV、車載、ウィジェットの呼び出し元も検索してください。
- `BuildConfig`、Data Binding、Room/KSP 等の生成コード、`build/`、`.gradle/`、`.dbg/`、JNI の `obj/`/`libs/` は直接修正しないでください。
- SDK/NDK の絶対パス、署名鍵、パスワードなどのローカル設定をコミットしないでください。
- `libvlcjni` や取得した medialibrary ソースは独立したリポジトリです。親の `git diff` に現れない変更があるため、編集したリポジトリごとに差分を確認してください。継続利用する外部修正は対応するパッチ運用も確認してください。
- このフォークの `origin` は GitHub、`upstream` は VideoLAN GitLab です。提出先は依頼に合わせて判断してください。上流への Merge Request は VideoLAN GitLab に提出し、説明の形式は `.gitlab/merge_request_templates/merge_request.md` を参考にし、変更理由と検証結果を記載してください。

## このチェックアウトで確認したビルド上の注意

以下は 2026-10-20 のビルド調査で見つかった事項です。2026-10-23 に該当する生成規則と変数名の不一致が残っていることをソースで再確認しています。今回、ビルド自体は再実行していません。現在の設定とログを確認してください。

1. `msgfmt` が存在せず VLC の configure が `MSGFMT=:` を設定した場合、`libvlcjni/vlc/share/Makefile.am` の appdata XML 生成規則が何も生成せず成功します。後続の `make install` が `org.videolan.vlc.appdata.xml` 不在で失敗します。`MSGFMT=false` を指定してテンプレートのコピーへ進めると、この生成・インストール処理が成功することを確認しています。恒久対応は出力の存在を確認する生成規則、または gettext を用意したうえでの再 configure を検討してください。これだけで全ビルドの成功を保証するものではありません。
2. `buildsystem/compile.sh` の既存 wrapper 確認では、`GRADLE_CACHED_VERSION` に取得した値を `GRADLE_PATH_VERSION` で比較しています。値が一致せず wrapper が不要に削除・再生成される原因になるため、Gradle の再取得が続くときはこの変数名を確認してください。
