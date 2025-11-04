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
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import jp.ac.neec.it.k025c6004.boookmemory.db.BookDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SearchActivity : AppCompatActivity() {
    private lateinit var editQuery: EditText
    private lateinit var btnSearch: Button
    private lateinit var listView: ListView
    private val items = mutableListOf<SearchItem>()//ここのサーチアイテムってのは後で出てくるGoogleのAPIね
    //こっちが生きたデータ
    private val dataList = mutableListOf<Map<String, String>>()
    //こっちがリスト用のデータ

    private lateinit var adapter: SimpleAdapter//いつもの通りアダプター作り

    // タイトルともう一個表示マッピンググググ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.researchresult) //

        editQuery = findViewById(R.id.bookkensaku)//UIと結べ
        btnSearch = findViewById(R.id.kensakubutton)
        listView = findViewById(R.id.researchResult)

        adapter = SimpleAdapter(//リストビューセット
            this,
            dataList,
            android.R.layout.simple_list_item_2,//２つの値のリストを表示しようね
            arrayOf("title", "authors"),//ここでどのキーの値を使うか指定。
            intArrayOf(android.R.id.text1, android.R.id.text2)
            //左から上下にそれぞれタイトルauthorsが入るってわけ。パースGoogleなんちゃらに値の正体あるよ。
        )
        listView.adapter = adapter//アダプターセット

        btnSearch.setOnClickListener {
            val q = editQuery.text.toString().trim()
            //エディットの中身をeditableから文字列に前後の空白やら消すキミケス
            if (q.isNotEmpty()) searchBooks(q)
            //空文字じゃなければq渡してサーチブック起動
        }
        // 送信キーで検索出来るためのもの。
        editQuery.setOnEditorActionListener { _, actionId, _ ->
            //何でこう書くの？　エンターキーでも動くように。エディター内での挙動を設定。
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                //キーボードの検索ボタンが押されたことを検知
                //actionIDでトリガーかどうか判定
                btnSearch.performClick()//検索を直接呼び出す。
                true//ここでtrueにすると終わり。
            // やらないと改行も同時に起きるっぽい？それはそれでおもろいけど要らないっしょ
            } else false
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val sel = items[position]
            // items は SearchItem の値を渡すから。。
            //あーそっかたんに表示用のデータはキー文字列なので使えん。
            //なので普通にitemでその位置の物を渡す必要があるんですね。
            //主キーとかも一切要らないのでポジションだけください。
            //我々は何も望みません。清貧を愛せ。
            val i = Intent(this, EditActivity::class.java)
                .putExtra(Nav.EXTRA_BOOK_ID, sel.bookId)//それぞれ一時的に使う部分に指定の値突っ込んでる。
                //エディット側だとEXTRA_BOOK_IDの中身がsel.bookIdになってる。
                .putExtra(Nav.EXTRA_TITLE, sel.title)
                .putExtra(Nav.EXTRA_AUTHORS, sel.authors)
            startActivity(i)
        //取った値をインテントに詰め込んでactivity起動
            //intent.getStringExtra(...)これで向こうで受け取る。
        }
    }

    private fun searchBooks(query: String) {
        val url = buildUrl(query)//API検索用のURLをここで作る

        // SimpleAdapter用のデータをいったんクリア（UIスレッド）
        dataList.clear()//前の検索結果クリア
        adapter.notifyDataSetChanged()//データ消えたから前の検索結果を除外！！オシリスの天空竜かなぁ！？
        items.clear()
        //本体のさっき作ったitemsのデータリストを除外。所詮一時しか使わん。
        //ここでなになぜ？データベースのコーナー！！！
        //どうしてアイテムなんて作ったの？？マスターの方に直接突っ込んでそこを参照すればよいではないか？
        //答え　バカ言うんじゃねぇもし途中で登録止める気になったらどうすんだ一貫性消えるやろ
        //一々マスターをいじるバカがどこにいるよ


        //ここからコルーチン相変わらずよくわかんないねぇ！？
        lifecycleScope.launch(Dispatchers.IO) {//ネットワーク向きのスレッド起動。アクティビティー雑に終わらせても一緒に死んでくれていいようにlifecycleで呼ぶ。
            try {
                val json = httpGet(url)
                //下にある中身のテキストをURL突っ込んで帰ってきた値をjsonに入れて
                val parsed = parseGoogleBooks(json)
                //リクエストして帰ってきたJSON文字列の受け口
                //JSONを解析してもらおうか。parseGoogleBooksは下の受け取るときを参照されたし
                withContext(Dispatchers.Main) {
                    //UIをいじるためにメインスレッドにおかえり願おう
                    // 何でいじれねぇんだよ！！！！！！殺されてぇのか！！！！！！
                    // itemsにパースだかバース高突っ込みますラップかなぁ！？
                    items.addAll(parsed)
                    //クリックされたら使おうね♡

                    // 表示用のdatalistに title / authors を流し込む
                    dataList.clear()
                    //さっきのパースをマップ用に洗脳改造していれます。
                    //おい見ろよ！！こいつ我を忘れてリストになっちまいやがったぜ　へへっ
                    dataList.addAll(parsed.map { mapOf(
                        "title" to it.title,
                        "authors" to it.authors
                    )})

                    adapter.notifyDataSetChanged()//アダプタに変更を通知してリストを変更させる

                    if (parsed.isEmpty()) {
                        //０件なら無いよって教えようねシュプリームの店員
                        Toast.makeText(this@SearchActivity, "見つかりませんでした", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {//エラー処理。いるか？？？おめいさん
                withContext(Dispatchers.Main) {//UI表示にここでも戻す。
                    Toast.makeText(this@SearchActivity, "検索に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun buildUrl(q: String): String {
        val encoded = URLEncoder.encode(q, Charsets.UTF_8.name())
        //入ってきたQ(URL)が入るようにUTFにしていれてる。
        // タイトル/著者をurｌに変換して広く検索。こいつで検索語をURLに直してやる。
        //分からせ行為
        return "https://www.googleapis.com/books/v1/volumes?q=$encoded&maxResults=20"
    }

    private fun httpGet(urlStr: String): String {
        val url = URL(urlStr)//アクセス先をここに置いておく
        //下でURL開くようにしてる。ここのオープンコネクション。
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000//時間とかタイムアウトとかメソッドとか開く際の制約とか設定
            readTimeout = 8000
            requestMethod = "GET"
        }
        //接続するためのもの。８秒の間にメソッドgetつまり読み取りでデータもーらい
        conn.inputStream.bufferedReader().use { br ->
            val text = br.readText()
            //テキストに入力されたデータ全部突っ込んだ。この後読み取ってもらう
            conn.disconnect()//ここで通信を閉じてる。
            return text
        }//受け取ったデータbrをテキストにしていく。useでちゃんと閉じてくれるらしいよ。
    }
    //この下は簡単に言うと受け取ったJSONを解析してサーチアイテムのリストに突っ込むやつ。
    private fun parseGoogleBooks(json: String): List<SearchItem> {
        val root = JSONObject(json)//ここで文字列をデータに変換
        if (!root.has("items")) return emptyList()//結果が空なら空リスト
        val res = mutableListOf<SearchItem>()//resっていうリストを作ってる。サーチした結果を突っ込む。
        val arr = root.getJSONArray("items")
        for (i in 0 until arr.length()) {//取れた情報の長さの分回す。
            val item = arr.getJSONObject(i)//arrの中のi番目の物取ってくる。
            val id = item.optString("id") // Google Books の ID
            val volume = item.optJSONObject("volumeInfo") ?: continue
            //凝れなかったらスキップ
            val title = volume.optString("title").ifBlank { "(無題)" }
            //タイトルからなら無題
            val authors = if (volume.has("authors")) {
                //著者ををカンマ区切りで入れてやる
                val a = volume.getJSONArray("authors")
                (0 until a.length()).joinToString(", ") { a.getString(it) }
            } else ""//無けりゃ空文字
            res.add(SearchItem(id, title, authors))
            //リストに追加。
        }
        return res//resを返す
    }
}
