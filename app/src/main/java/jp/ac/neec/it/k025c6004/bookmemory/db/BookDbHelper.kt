// jp/ac/neec/it/k025c6004/boookmemory/db/BookDbHelper.kt
package jp.ac.neec.it.k025c6004.boookmemory.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BookDbHelper(context: Context) :
//よくわかんねぇけどまぁ継承してる。ここから書き込みか不可をつっこんでる
    SQLiteOpenHelper(context, "books.db", null, 1) {
        //どの環境で作るかコンテキスト
        // （アクティビティーとかバックグラウンドとか全部）。リソースにアクセスする。ファイルをアプリ内に作る。
        //名前、ファクトリーってなんだ？よくわかんねぇ。使わん、
        // データベースのバージョン。
        // 管理でしか使わねぇだろsqライトってデータベースで作るよ

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
          CREATE TABLE books (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,  -- ListView用の主キー
            book_id TEXT UNIQUE,                    -- ISBN or GoogleBooks ID（論理ID）
            title TEXT,
            authors TEXT,
            publisher TEXT,--出版社
            publishedDate TEXT,--日付。未使用
            description TEXT,--本の説明。未使用
            thumbnailUrl TEXT,--表紙画像。未使用。
            price TEXT,--値段。あったりなかったりするらしく未使用。
            memo TEXT,--我々が突っ込むメモ。
            review TEXT,--使ってない。レビューいれる予定はない。感想と分けてない。
            rating INTEGER,--評価を星でつけようと思った。めんどうで断念
            status TEXT     -- "want" or "read"欲しい本か読んだ本化。
          );
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_books_status_title ON books(status, title);")
            //インデックスを作ってる。検索する時に早くなる、、、
    // らしい。一回一回引っ張るコードなんて書いてられるもんですか
        //ステータスでグループを作ってタイトルを並べてる。
        //WHERE status=? AND title LIKE が早くなる。Bツリーとか言う奴だっけか
        //裏で保存してる。
        //SQL文を投げる命令で作ってます。
    }
//列を削除とか追加しようとするときに使うらしい。知らねぇよ。
    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {

    }
}
