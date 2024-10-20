package fett.scoring

import eu.mihosoft.ext.apted.costmodel.CostModel
import eu.mihosoft.ext.apted.distance.APTED
import eu.mihosoft.ext.apted.node.Node
import fett.sexp._
import fett.util.logger
import fett.util.utils.Implicits._
import fett.util.hashConsing._
import fett.util.trees._
import play.api.libs.json.Json

import collection.concurrent.TrieMap
import java.nio.file.{Files, Paths}
import java.nio.charset.Charset

case class TreeEditDistance(
  matchScore: Double = 0,
  mismatchScore: Double = 1,
  insertionScore: Double = 1,
  deletionScore: Double = 1,
  eqClasses: Option[Set[Set[Symbol]]] = None,
  similarityFn: SmithWaterman.SimilarityScore = SmithWaterman.naiveNodeNameSimilarity,
  weights: Option[Map[Set[Symbol], Double]] = None) extends Scoring {

  // BEGIN BLATANT PLAGIARISM FROM SMITH WATERMAN

  val useClassBasedSimilarity = similarityFn == SmithWaterman.classBasedNodeNameSimilarity

  val eqClasses_ = if (useClassBasedSimilarity) {
    eqClasses.getOrElse(sys.error("Tried to use class-based similarity without similarity classes"))
  } else {
    Set.empty[Set[Symbol]]
  }

  // equivalence classes are disjoint so instead of taking intersection, we can just check for equality
  // for faster equality check, use automatically generated symbols
  val uselessNodeClassNo = Symbol("-1")
  // create a representative for each node class
  val nodeClasses = eqClasses_.zipWithIndex.toMap.mapValues((x:Int) ⇒ Symbol(x.toString)).withDefaultValue(uselessNodeClassNo)

  val nodeNameToClassNo: Map[Symbol, Symbol] = (for {
    c ← eqClasses_
    sym ← c
  } yield {
    sym → nodeClasses(c)
  }).toMap.withDefaultValue(uselessNodeClassNo)

  val classNoToWeights: Map[Symbol, Double] = (for {
      (set, weight) <- weights.getOrElse(Map()).toList
      nodeName <- set
      classNo = nodeNameToClassNo(nodeName)
    } yield (classNo -> weight)
  ).toMap.withDefaultValue(1.0)

  val nodeSimilarity = similarityFn(matchScore, mismatchScore, classNoToWeights)

  // END BLATANT PLAGIARISM FROM SMITH WATERMAN

  val selfSimilarity = TrieMap.empty[ParseTreeNode, Double]

  def replaceParseTreeLabelsWithEquivalenceClassNames(n: ParseTreeNode): ParseTreeNode = {
    // fix children
    val newChildren = n.children.map(replaceParseTreeLabelsWithEquivalenceClassNames(_))

    n.copy(label=nodeNameToClassNo(Symbol(n.label)), children=newChildren)
  }

  def removeUselessNodes(n: ParseTreeNode): ParseTreeNode = {

    // either you get back a new node with filtered children, or the node itself was useless and you just get back the filtered children
    def helper(n: ParseTreeNode): Either[ParseTreeNode, IndexedSeq[ParseTreeNode]] = {
      // convert children
      val newChildren = n.children.map(helper(_)).flatMap(x => x match {
          case Left(childNode) => IndexedSeq(childNode)
          case Right(childNodes) => childNodes
        })

      val isLeaf = n.children.length == 0

      val doWeCareAboutTerminals = true

      if (doWeCareAboutTerminals) {
        if (!isLeaf) {
          (n.label == uselessNodeClassNo) match {
            case true => Right(newChildren)
            case false => Left(n.copy(children=newChildren))
          }
        } else {
          // keep the leaf and split up the tokens
          Right(Mining.splitToken(n.label).map(l => n.copy(label=l)).toIndexedSeq)
        }
      } else {
        (n.label == uselessNodeClassNo) match {
          case true => Right(newChildren)
          case false => Left(n.copy(children=newChildren))
        }
      }
    }

    helper(n) match {
      case Left(n) => n
      case Right(ns) => 
        // the root was useless; let's just keep it so that we don't make an arbitrary choice for the new root
        n.copy(children=ns)
    }
  }

  def parseTreeToLblTree(n: ParseTreeNode): Node[Symbol] = {
    // make child nodes
    val childTrees = n.children.map(parseTreeToLblTree(_))

    val t = new Node(Symbol(n.label))
    childTrees.foreach(t.addChild(_))

    t
  }

  override def importantNodeLabels: Set[String] = Set("antlrparsers.java.JavaParser$ReturnStatementContext")

  def fixParseTrees(n: ParseTreeNode): ParseTreeNode =
    if (useClassBasedSimilarity)
      removeUselessNodes( replaceParseTreeLabelsWithEquivalenceClassNames(n) )
    else
      n

  class OurCostModel(insCost: Double, delCost: Double, matchScore: (Symbol, Symbol) => Double) extends CostModel[Symbol] {
    override def del(n: Node[Symbol]): Float = delCost.toFloat
    override def ins(n: Node[Symbol]): Float = insCost.toFloat
    override def ren(n1: Node[Symbol], n2: Node[Symbol]): Float = matchScore(n1.getNodeData, n2.getNodeData).toFloat
  }

  def similarity(_a: ParseTreeNode, _b: ParseTreeNode): Result[LineNo] = {
    // get the equivalence class trees for _a and _b
    val (a, b) = (fixParseTrees(_a), fixParseTrees(_b))

    val (ta, tb) = (parseTreeToLblTree(a), parseTreeToLblTree(b))
    // println(ta)
    // println(tb)

    val apted = new APTED[OurCostModel, Symbol](new OurCostModel(insertionScore, deletionScore, (x, y) => nodeSimilarity(x, y)))
    val α = math.max(insertionScore, deletionScore).toDouble

    def TES(t1: Node[Symbol], t1size: Double, t2: Node[Symbol], t2size: Double): Double = {
      val dist = apted.computeEditDistance(t1, t2)
                                          (α * (t1size + t2size) - dist) / 2.0
    }

    val ta_tb = TES(ta, a.size, tb, b.size)
    val ta_ta = selfSimilarity.getOrElseUpdate(_a, TES(ta, a.size, ta, a.size))
    val tb_tb = selfSimilarity.getOrElseUpdate(_a, TES(tb, b.size, tb, b.size))

    val TED = (ta_ta + tb_tb - 2.0 * ta_tb) / (ta_ta + tb_tb - ta_tb)

    Result(1.0 - TED, Set())
  }

  override val collapseTrees = true
}

object TreeEditDistance {

  def fromOptions(options: Map[String, String]) = {
    val matchScore = options.get("matchScore").fold[Double](0)(_.toDouble)
    val mismatchScore = options.get("mismatchScore").fold[Double](1)(_.toDouble)
    val insertionScore = options.get("insertionScore").fold[Double](1)(_.toDouble)
    val deletionScore = options.get("deletionScore").fold[Double](1)(_.toDouble)
    val similarity = options.getOrElse("similarity", "naive")
    val similarityFn = similarity match {
      case "tokenName" | "naive" ⇒ SmithWaterman.naiveNodeNameSimilarity
      case "classBased" ⇒ SmithWaterman.classBasedNodeNameSimilarity
      case "levenshtein" ⇒ SmithWaterman.levenshteinBasedNodeNameSimilarity
      case _ ⇒ sys.error(s"Unknown similarity metric name: '$similarity'")
    }
    val eqClasses = options.get("classFile") map {
      f ⇒
      val contents = new String(Files.readAllBytes(Paths.get(f)), Charset.forName("UTF-8")).replaceAll("//.*", "")
      // we assume that the JSON file is structured as follows:
      // {
      //   useless: ["token1", "token2", ...],
      //   class1: ["token1'", "token2'", ...],
      //   class2: ["token1''", "token2''", ...]
      // }
      //
      try {
        // if the condition above is not satisfied, we will get a casting error at some point
        val classes = Json.fromJson[Map[String, Seq[String]]](Json.parse(contents)).get
        val eqClasses = for ((name, tokens) ← (classes - "weights")) yield {
          val toks = tokens.map { t ⇒ Symbol("antlrparsers." + t + "Context") }
          name → toks.toSet
        }
        val eqClassMap = eqClasses.toMap
        val useless = eqClassMap.getOrElse("useless", Set.empty)
        val _weights = if (classes contains "weights") classes("weights").asInstanceOf[Map[String, Double]] else Map()
        val weights = _weights.map({ case (k, v) => eqClassMap(k) -> v })
        ((eqClassMap - "useless" - "weights").values.toSet, useless, weights)
      } catch {
        case e: ClassCastException ⇒
          sys.error(s"JSON file '$f' describing the equivalence sets is ill-formatted")
      }
    }
    val classes = eqClasses.map(_._1)
    val useless = eqClasses.map(_._2)
    val weights = eqClasses.map(_._3)
    TreeEditDistance(matchScore, mismatchScore, insertionScore, deletionScore, classes, similarityFn, weights)
  }
}
