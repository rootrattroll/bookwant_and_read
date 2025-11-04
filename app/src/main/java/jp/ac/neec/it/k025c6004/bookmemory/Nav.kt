package jp.ac.neec.it.k025c6004.bookmemory
//何でいるの？？？　変更途中の値を突っ込んで後からデータベースににゅるにゅるするため
object Nav {
    const val EXTRA_ROW_ID = "row_id"
    const val EXTRA_BOOK_ID = "book_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_AUTHORS = "authors"
    const val EXTRA_DELETED = "deleted"
}
data class SearchItem(
    val bookId: String,
    val title: String,
    val authors: String // カンマ区切りでOK
)
