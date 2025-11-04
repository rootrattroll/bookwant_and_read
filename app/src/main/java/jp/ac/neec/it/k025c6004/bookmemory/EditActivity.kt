package jp.ac.neec.it.k025c6004.bookmemory

import android.os.Bundle
import android.os.FileObserver.DELETE
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import jp.ac.neec.it.k025c6004.boookmemory.db.BookDbHelper

class EditActivity : AppCompatActivity() {//ここは変わらんやろ
    private val helper by lazy { BookDbHelper(this) }
    private var rowId: Long? = null//ここの有無で編集モードか否か決定される。

    private lateinit var titleView: TextView
    private lateinit var authorView: TextView
    private lateinit var memoView: EditText
    private lateinit var statusSwitch: Switch
    private lateinit var saveBtn: Button
    private lateinit var deleteBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_book_memo)

        titleView = findViewById(R.id.bookTitle)
        authorView = findViewById(R.id.author)
        memoView = findViewById(R.id.memo)
        statusSwitch = findViewById(R.id.switch1)
        saveBtn = findViewById(R.id.tourokubutton)
        deleteBtn = findViewById(R.id.delete)
        //既存の本ならrowIDを渡すので渡されたならそれをセットする。インテントからNav.EXTRA_ROW_ID探してtakeIFでもし満たさないならnullを返す。
        rowId = intent.getLongExtra(Nav.EXTRA_ROW_ID, -1L).takeIf { it >= 0 }

        if (rowId != null) {//nullじゃないなら
            loadExisting(rowId!!)//読み込んで絶対ヌルじゃない元がヌルじゃないって宣言してるから
            //ヌルじゃなければ既存のデータを取って表示する方を。
            deleteBtn.visibility = View.VISIBLE//削除ボタン見えるようにしようね
        } else {
            //検索から来る。 EXTRA_TITLE / EXTRA_BOOK_ID をセットしておく
            //インテントって何だよ　画面作るときにアクティビティーに値渡したりするやつだよぼけ
            titleView.text = intent.getStringExtra(Nav.EXTRA_TITLE) ?: ""
            authorView.text = intent.getStringExtra(Nav.EXTRA_AUTHORS) ?: ""
            deleteBtn.visibility = View.GONE//削除ボタンはファッキューレイアウトからもアッチ行ってて！
        }

        saveBtn.setOnClickListener { save() }

        deleteBtn.setOnClickListener{ confirmDelete()}
        //ボタンにevent設定
    }

    private fun loadExisting(id: Long) {//ここではデータベース取り出して中身を反映
        val db = helper.readableDatabase//一応読むだけのね
        db.rawQuery("SELECT * FROM books WHERE _id = ?", arrayOf(id.toString())).use { c ->
            //sqlを直接発行。？にidを突っ込め。?の中にはidがストリングで入ります。
            //その結果をcに突っ込んで、、、下で回す。ラムダ式最強！！！！ほらこれを読んでるあなたも！！！
            if (c.moveToFirst()) {
                //何で頭だけなの？cursorは全体持ってて指定してやらないと詳しく参照できません事よ
                //そこでどうせidはユニークなので頭だけ指定して取ればよいことですわー
                titleView.text = c.getString(c.getColumnIndexOrThrow("title"))
                authorView.text = c.getString(c.getColumnIndexOrThrow("authors")) ?: ""
                memoView.setText(c.getString(c.getColumnIndexOrThrow("memo")) ?: "")
                val st = c.getString(c.getColumnIndexOrThrow("status")) ?: "want"
                //stにステータス突っ込んで、
                statusSwitch.isChecked = (st == "want")//ステータス通りにスイッチを設定。でふぉるとはおふらしい？
            }
        }
    }

    private fun save() {
        val db = helper.writableDatabase//書き込み可能なｄｂの接続起動！！
        db.beginTransaction()//途中で落ちてもいいようにする仕組み。トランザクションというやつだな。
        // 成功か失敗かでーっどおああらーいぶデータの一貫性保つため
        try {
            if (rowId == null) {//新規か否かの判定。ここは新規
                //ここのコンパイルはアプリが落ちるまで使いまわせる？らしい。要はインサートの部分な
                db.compileStatement("""
                INSERT INTO books (book_id, title, authors, memo, status)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()).apply {
                //？は左からindex1,2,3,4,5ってなってる。
                // applyで纏めて書いてやるよ喰らえ
                //valueでつけた先頭の余計な空白を消し炭にしてやろうね！！
                // まぁぶっちゃけなくてもいいけどあったほうがいいらしい
                //左から上に入った値を突っ込む突っ込む値は下の順番通り
                //bindStringはSQLの何番目に名に突っ込むかってやつ。？：に関してはnullの場合は右の値を突っ込んで。
                //まぁぶっちゃけgoogleAPIからかならず値取るから何でもいいんですけど
                    bindString(1, intent.getStringExtra(Nav.EXTRA_BOOK_ID) ?: titleView.text.toString())
                    bindString(2, titleView.text.toString())
                    bindString(3, authorView.text.toString())
                    bindString(4, memoView.text.toString())
                    bindString(5, if (statusSwitch.isChecked) "want" else "read")
                }.executeInsert()//新規追加実行。今回はここで終わるようにしてるからあんま意味ない。
            } else {
                //ハイ上と一緒でSqL組んで、(今回は whereがあるけどね)
                //ブンポウ、チガウオレコレキライIDチガウシカタナイ。ウルサイ
                db.compileStatement("""
                UPDATE books SET title=?, authors=?, memo=?, status=? WHERE _id=?
            """.trimIndent()).apply {
                    bindString(1, titleView.text.toString())
                    bindString(2, authorView.text.toString())
                    bindString(3, memoView.text.toString())
                    bindString(4, if (statusSwitch.isChecked) "want" else "read")//本当ならこいつはbooleanの1と0でよかった。
                    bindLong(5, rowId!!)
                }.executeUpdateDelete()//削除や更新実行。既存行に
            }
            db.setTransactionSuccessful()//落ちてもいいようにデータの一貫性保つため！！
        } finally { db.endTransaction() }//落ちてもいいように
        setResult(RESULT_OK); finish()//コールバックだかキックバックだか終わったぞキリッ
    }

    private fun confirmDelete() {//デリートするかの確認。ダイアログAPIね！
        AlertDialog.Builder(this)
            .setTitle("削除しますか？")
            .setMessage("この本の記録を削除します。取り消せません。")
            .setPositiveButton("削除") { _, _ -> deleteRow() }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun deleteRow() {//ここで完全に削除作曲家の方じゃないよ
        val id = rowId ?: return//万が一編集対象行番号ID無ければリターン。値はここを起動する時にもらってる。ここら辺はその方が安全だから。
        val db = helper.writableDatabase//かけるデータベースとして起動
        db.beginTransaction()//処理開始一貫性を愛せ。一途な愛でしょ
        try {
            db.compileStatement("DELETE FROM books WHERE _id = ?")
                .apply { bindLong(1, id) }
                .executeUpdateDelete()//削除を実行
            db.setTransactionSuccessful()//成功したら確定
        } finally {
            db.endTransaction()//失敗したらここで必ず潰す。メモリさん逃げていいよ
        }
        val data = Intent().putExtra(Nav.EXTRA_DELETED, true)//メインに通知するためにobjectを変更して渡す。
        setResult(RESULT_OK, data)//削除完了したらやっぱりメインに渡すためにリザルトオッケー
        finish()//ここで閉じて向こうのコールバック起動
    }
}
