# 📚 BookMemory – オフライン読書メモ & 積読管理アプリ
Android（Kotlin）で開発した、読みたい本／読んだ本をシンプルに管理するアプリ。
Google Books で検索 → 1タップで登録、メモやステータスを編集できます。
検索以外はオフライン動作（SQLite）で高速＆軽量。
# 主な機能

🔎 本を検索（Google Books API / タイトル・著者）
検索結果はタイトル＋著者の2行表示。タップで編集画面へ。

➕ 新規登録
検索結果から book_id / title / authors を引き継いで SQLite に保存。

📝 編集
タイトル・著者・メモ、ステータス（want / read）を編集。
レコード削除（確認ダイアログ付き）にも対応。

🗂 リスト表示 & 絞り込み
ホームで「読みたい / 読んだ」をタブ切替、上部のテキストでタイトル部分一致検索。

💾 完全オフライン閲覧
検索以外は SQLite のローカルデータのみで動作。
# 🖼 画面構成

1 ホーム（メイン）

  リスト：タイトル＋著者（SimpleCursorAdapter / simple_list_item_2）

  ボタン：読みたい / 読んだ 切替、検索、本を追加

  入力：タイトル検索欄

2 検索画面

  テキスト＋ボタンで検索

  結果リスト（タイトル＋著者）

  タップで EditActivity へ Intent で値を受け渡し

3 編集画面

  タイトル、著者、メモ、ステータス（Switch）

  登録／更新ボタン、削除ボタン（要確認）

# 🏗 アーキテクチャ概要

UI：Activity + XML（ListView / EditText / Button / Switch）

非同期：lifecycleScope + Dispatchers.IO（検索時のHTTP）
UI 更新時は Dispatchers.Main に切替

データベース：SQLiteOpenHelper（BookDbHelper）
リスト表示は SimpleCursorAdapter を使用
adapter.changeCursor(newCursor) で差し替え（旧カーソルは自動 close）

画面間データ受け渡し：Intent.putExtra(...)（Nav 定数でキーを集約）
# 📂 プロジェクト構成（例）

※ パッケージ名はプロジェクトに合わせて調整してください
```
app/
├─ src/main/java/＜your.package＞/bookmemory/
│  ├─ MainActivity.kt          # ホーム（一覧・検索ボックス・ステータス切替）
│  ├─ SearchActivity.kt        # Google Books 検索（HTTP + JSON パース）
│  ├─ EditActivity.kt          # 登録/更新/削除
│  ├─ model/
│  │   └─ SearchItem.kt        # 検索結果の一時モデル（id/title/authors）
│  └─ db/
│      └─ BookDbHelper.kt      # SQLiteOpenHelper（スキーマ管理）
│
└─ src/main/res/layout/
   ├─ activity_main.xml        # ホーム
   ├─ researchresult.xml       # 検索
   └─ create_book_memo.xml     # 編集

```

---

# 🗃 データベース（SQLite）スキーマ
```
CREATE TABLE books (
  _id INTEGER PRIMARY KEY AUTOINCREMENT, -- ListView用ID（必須）
  book_id TEXT UNIQUE,                   -- Google BooksのIDやISBN等（論理ID）
  title TEXT,
  authors TEXT,
  publisher TEXT,
  publishedDate TEXT,
  description TEXT,
  thumbnailUrl TEXT,
  price TEXT,
  memo TEXT,
  review TEXT,
  rating INTEGER,
  status TEXT                            -- "want" or "read"
);

CREATE INDEX idx_books_status_title ON books(status, title);

```

---

SimpleCursorAdapter は _id 列が必須

status + title の複合インデックスで、フィルタ＋並び替えが高速化

# 🧰 技術スタック
Kotlin / AndroidX

SQLite（SQLiteOpenHelper / SimpleCursorAdapter）

HTTP：HttpURLConnection（Retrofit不使用）

JSON：org.json.JSONObject

非同期：lifecycleScope + Dispatchers.IO/Main

# ▶ ビルド & 実行
Android Studio（Giraffe 以降推奨）

minSdk / targetSdk はプロジェクトに合わせて設定

パーミッション（検索で使用）
```
<uses-permission android:name="android.permission.INTERNET"/>

```

---
# 📌 設計メモ

カーソル管理：
クラスに cursor を持たず、changeCursor(newCursor) に統一（旧カーソルは自動 close）。
onDestroy() は adapter.changeCursor(null) で安全に解放。

戻り制御：
画面遷移は startActivity()。戻り後のリロードは onResume() に集約（ActivityResultLauncher 不要）。

安全なクエリ：
selection + selectionArgs で SQLインジェクション対策。

一時モデル：
表示用 Map<String, String> と、次画面に渡す SearchItem（型付き）を分離。

# 🔮 今後の拡張

📷 バーコード読み取り（ISBN → 自動検索）

ML Kit Barcode / ZXing など

🖼 サムネイル表示（Picasso/Coil 等で thumbnailUrl を表示）

💵 価格・在庫（外部APIの整備があれば）

⭐ 評価 UI（rating の活用）

🔁 バックアップ/復元（JSON エクスポート）


  
