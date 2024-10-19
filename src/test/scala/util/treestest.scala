import fett.util.trees._
import org.scalatest.funsuite.AnyFunSuite

object TreeData {
	val T1 = Tree("a", IndexedSeq(Tree("b", IndexedSeq(Empty)),
		Tree("c", IndexedSeq(Empty))))

	val T2 = Tree("a", IndexedSeq(Tree("b", IndexedSeq(Empty)),
		Tree("c", IndexedSeq(Empty))))
}

class TreesSuite extends AnyFunSuite {

	import TreeData._

	test("compare two similar trees") {
		val score = Tree.α(T1, T2)
		assert(score == 0)
	}

	test("compare two different trees") {
		val T1 = Tree("a", IndexedSeq(Tree("b", IndexedSeq(Empty)),
			Tree("c", IndexedSeq(Empty))))
		val T2 = Tree("e", IndexedSeq(Tree("f", IndexedSeq(Empty)),
			Tree("g", IndexedSeq(Empty))))
		val score = Tree.α(T1, T2)
		assert(score == 3)
	}
}
