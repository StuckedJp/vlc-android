# 俺専用 VLC for Android

## 概要

このリポジトリは VLC for Android に、私の趣味に応じた機能追加と仕様変更を施したものです。変更は Codex にやってもらっています。以下の変更を施しています。

* UPnP 上の MPEG2-TS コンテンツへのシーク対応。
* リモコンの左右キーでのシークを 15 秒に固定。
* 複数のストリームを持つ MPEG2-TS コンテンツに、ストリームを選択する機能を追加。
* 頭文字ごとのリスト見出しをデフォルトで非表示。
* 再生位置をプロセス内だけで保持し、プロセス終了後は先頭から再生。
* 動画の再生画面からコンテンツリストへ戻ったときにリピートモードを解除。
* 動画の連続再生で次のコンテンツへ進むときは、保存された再生位置によらず先頭から再生。
* TS の連続性カウンター検査を既定で無効化し、パケット欠損のある放送録画でも映像の継続表示を優先（`--no-ts-cc-check`）。重複パケットの除去も無効になるため、必要な場合はカスタム libVLC オプションの `--ts-cc-check` で検査を再有効化。


## 依存リポジトリ

以下のリポジトリに依存しています。「Upstream」は fork 元のリポジトリです。

* [libvlcjni](https://github.com/StuckedJp/libvlcjni)
    * [Upstream](https://code.videolan.org/videolan/libvlcjni)
* [VLC](https://github.com/StuckedJp/vlc)
  * [Upstream](https://code.videolan.org/videolan/vlc)
* [medialibrary](https://github.com/StuckedJp/medialibrary)
    * [Upstream](https://code.videolan.org/videolan/medialibrary)
* [libvlcpp](https://github.com/StuckedJp/libvlcpp)
    * [Upstream](https://code.videolan.org/videolan/libvlcpp)


## ビルド手順

公式のビルド手順をご覧ください。Dev ビルドが必要です。
