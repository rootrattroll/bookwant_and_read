package jp.ac.neec.it.k025c6004.bookmemory

import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import jp.ac.neec.it.k025c6004.boookmemory.db.BookDbHelper
import android.content.Intent
import android.view.View
import android.widget.AdapterView


class MainActivity : AppCompatActivity() {//ライフサイクル開始
    //private val helper by lazy { BookDbHelper(this) }
    private val helper = BookDbHelper(this@MainActivity)
    //遅れてインスタンス化をする。値が準備できなかったりするとヌルで落ちる。あとで必ず準備するって約束。
    private lateinit var list: ListView//アクティビティー宣言遅延初期化して後で。リロードリストとかで使えるように一番上で作ってる。
    private lateinit var search: EditText//こっちはセットコンテントビューを待ってる。？？らしい。
    //宣言後で代入するよってして落ちるの防止。
    //ｄｂヘルパー出来るまではまとう。
    //アクティビティー初期化前に突っ込むと入れる先がないよね
    // setContentView() したあとに findViewById() で実体を代入するために使ってる
    private var cursor: Cursor? = null
    //ここは普通に初期化。nullは許せ。cursorはセレクト結果を行単位で値が入る。最初のポインタは無くていいよね。
    // データベースの小さい番みたいな
    private var adapter: SimpleCursorAdapter? = null
    //いつもの通り仲介役。コイツがcursorをテキストビューに入れてくれる
    //クリエイト時にはまだCursor用意してないんだから無理だよなぁ？
    private var status = "want"
    // btnで "read" に切替

    private val editLauncher = registerForActivityResult(//ランチャーは一番上に必要らしい。
        //画面遷移があった時に下にある契約通りなら動け。(contract)
        ActivityResultContracts.StartActivityForResult()
      //別のアクティビティー開かれて閉じられて戻ってきたんだったら下を動かしてもらおう
        //javafx星人なのでラムダにした。意味はない。
    ) { result ->//画面が閉じられて向こう側の値がリザルトオッケーだったらリロードリストを呼べ
        if (result.resultCode == RESULT_OK) reloadList(search.text?.toString())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)//画面を設定

        list = findViewById(R.id.listBooks)//リストを宣言
        search = findViewById(R.id.kensakuword)//検索ボックス宣言

        list.onItemClickListener = ListItemClickListener()
        //リストにeventリスナ設定

        findViewById<Button>(R.id.btnWant).setOnClickListener { status = "want"; reloadList(search.text.toString()) }
        findViewById<Button>(R.id.btnRead).setOnClickListener { status = "read"; reloadList(search.text.toString()) }
        //ここでフラグの値の変更を設定。
        // 同時にリロードリストにサーチに入ってる値を突っ込む。一々空欄になるの鬱陶しいでしょう？
       findViewById<Button>(R.id.addbook).setOnClickListener {
            // 新規 → _idなしでEdit
           // iに放り込む値とか何処のインテント開くかをまとめてエディットランチャーに渡してる
            val i = Intent(this@MainActivity, SearchActivity::class.java)
            editLauncher.launch(i)
       //返ってきたオブジェクトを.launchこれで呼ぶと結果受け取ってくれるらしい。
        }
        findViewById<Button>(R.id.kensaku).setOnClickListener {
            reloadList(search.text.toString())
        //検索ボタンを押したらサーチをテキストに突っ込んでる。
        }


        reloadList()
    }
    //いつもの通りどのリストのビューでそいつは何番目？idは何？
    private inner class ListItemClickListener : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            val i = Intent(this@MainActivity, EditActivity::class.java)
                //情報渡して遷移先起動インテントに突っ込んでゴー
                //いつもの通り何処の画面開けって命令
                //putExtraで追加データを渡して開く
                .putExtra(Nav.EXTRA_ROW_ID, id)
            //Q 左おめぇだれだよカス A DBの行番号だよはったおすぞ。右誰だよ。リストの何番目かだよ殺すぞ
            editLauncher.launch(i)//インテントの値I突っ込んで開く。インテントに梱包された今のデータ
        //終わったら何するかのめいれいはさっきのリザルトのところに
        }
    }

//ここから検索画面とか読む読まない切り替え。引数に検索タイトルを。
    private fun reloadList(titleLike: String? = null) {
        cursor?.close()//ここではメモリ開放。前回の検索結果を消さないと。
        val db = helper.readableDatabase//何か一応読むだけが習慣らしい？
    //文字を作るパート。あとでSQLつくろうねぇー
        val sel = buildString {//
            append("status=?")
            //プレースホルダで安全にね。あとここでは、where句の中身をselに入れてる。
            // where status=?でステータス(読む読まないを後で入れる)
            if (!titleLike.isNullOrBlank()) append(" AND title LIKE ?")
            //もしタイトルが塗るじゃなければライクでつまりここまでで"status=? AND title LIKE ?"ができてる
        // 組み立てて
        }
        val args = mutableListOf(status).apply {//ここで初めてフラグが使われる。ステータスを先ほどの？にいれるために。。
            //一個目を先に、二個目を後ろに突っ込んでいる。そういう配列を作ってる。
            if (!titleLike.isNullOrBlank()) add("%$titleLike%")//検索ワードを後ろのはてなに。渡された引数が入る。
        }.toTypedArray()//あとのdb.queryがjavaなので必要らしい。

        cursor = db.query(//ここでWhere句を生成コイツを下でリストに渡してやる。順番通りね、、、？
            "books",//from
            arrayOf("_id", "title", "authors"),//セレクトの中に突っ込む
            sel,//ウェアー句
            args,//ここで具体的な値を順番にsutatus = want AND title LIKE %$titleLike%
            null,//使わん
            null,//使わん
            "title COLLATE NOCASE"//これで並び替え
        )

        if (adapter == null) {
            adapter = SimpleCursorAdapter(
                //ここで初回の時はアダプターがヌルなので不通にアダプター作る。リストセット
                this,
                android.R.layout.simple_list_item_2,
                cursor,
                arrayOf("title", "authors"),
                intArrayOf(android.R.id.text1, android.R.id.text2),
                //マッピングっていうんでしたっけ？あれ。何処の部品にどこ突っ込むか順番通りいれてる。
                0
            )
            list.adapter = adapter
        } else {//二回目は普通に突っ込んで更新
            adapter?.changeCursor(cursor)
        //アダプター内のcursorを新しくする。上で作ったやつ。アダプターを経由してリストビューに戻してやる。
        }
    }
    override fun onResume() {//ユーザーが見える場所に出る直前に起動。ちゃんと最新のリストをお届け
        super.onResume()
        reloadList(findViewById<EditText>(R.id.kensakuword).text?.toString())
    }


    override fun onDestroy() {//ここでいったんぶっ壊してライフサイクル終わり
        adapter?.changeCursor(null)//アダプタを空にして切り離す。
        cursor?.close()//dbを閉じてリソース開放
        helper.close()//dbを閉じようねメモリリークはやばい
        super.onDestroy()
    }
//    object Nav {ﾃｽﾄﾃﾞイタ
//        const val EXTRA_ROW_ID = "row_id"
//        const val EXTRA_BOOK_ID = "book_id"
//        const val EXTRA_TITLE = "title"
//    }
}
