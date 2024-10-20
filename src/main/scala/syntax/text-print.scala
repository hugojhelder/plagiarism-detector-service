package notjs.syntax
import notjs.translator.RunTranslator
import notjs.translator.PVarMapper
import java.io._

object TextPrint {

  def main(args: Array[String]) {
    assert(args.length >= 2, "Needs atleast 2 arguments: input js file name and output dot file name")

    val ast = RunTranslator.translateFileToProgram(new File(args(0)), args.toSet("--debug"))
    val mapping = PVarMapper.getMapping
    writeText(ast, mapping, args(1))
  }

  def writeText(a: AST, reverseMapping: Map[Int, String], file: String) {
    val out = new PrintWriter(new BufferedWriter(new FileWriter(file)))
  
    def strId(node: AST) = node.id.toString
    
    def getStr(n: Int) = reverseMapping(n)
  
    def outputBinding(depth: Int)(binding: Tuple2[PVar,Exp]): String = {
      val bindid = output(depth)(binding._1)
      val expid = output(depth)(binding._2)

      bindid
    }
  
    def scratchText(n: Int): String =
      "Scratch: "+ n.toString
  
    def matchBop(b: Bop): String = (b match {
      case ⌜+⌝ => "add"
      case ⌜−⌝ => "sub"
      case ⌜×⌝ => "mul"
      case ⌜÷⌝ => "div"
      case ⌜%⌝ => "mod"
      case ⌜<<⌝ => "lsh"
      case ⌜>>⌝ => "rsh"
      case ⌜>>>⌝ => "ursh"
      case ⌜<⌝ => "lt"
      case ⌜≤⌝ => "lte"
      case ⌜&⌝ => "band"
      case ⌜|⌝ => "bor"
      case ⌜⊻⌝ => "xor"
      case ⌜&&⌝ => "land"
      case ⌜||⌝ => "lor"
      case ⌜++⌝ => "cat"
      case ⌜≺⌝ => "lex"
      case ⌜≼⌝ => "lexe"
      case ⌜≈⌝ => "nse"
      case ⌜≡⌝ => "se"
      case ⌜⋆⌝ => "acc"
      case InstanceOf => "iof"
      case In => "in"
    }) 
    
    def matchUop(u: Uop): String = (u match {
      case ⌞−⌟ => "nneg"
      case ⌞~⌟ => "bneg"
      case ⌞¬⌟ => "lneg"
      case Typeof => "typeof"
      case ToBool => "tobool"
      case IsPrim => "isprim"
      case ToStr => "tostr"
      case ToNum => "tonum"
    }) 
  
    def printNode(depth: Int, id: String, label: String) {
      val prefix = "  "*depth
      val fpart = prefix + label
      val suffix = if(fpart.length<70) " "*(70-fpart.length) else ""
      out.println(fpart + suffix + "ID: " + id)
    }
    
    def output(depth: Int)(node: AST): String = {
      val myid = strId(node)
  
      node match {
        case Decl(bind, s) => {
          printNode(depth, myid, "Decl")

          val bindingIDs = bind map outputBinding(depth + 1)
          val sid = output(depth + 1)(s)

          myid
        }

        case SDecl(bound, s) => {
          printNode(depth, myid, "SDecl")
          output(depth + 1)(s)
          myid
        }
        case Seq(ss) => {
          printNode(depth, myid, "Seq")
          val ids = ss map output(depth + 1)
          myid
        }

        case If(e, s1, s2) => {
          printNode(depth, myid, "If")
          val eid = output(depth + 1)(e)
          val s1id = output(depth + 1)(s1)
          val s2id = output(depth + 1)(s2)
          myid
        }

        case While(e, s) => {
          printNode(depth, myid, "While")
          val eid = output(depth + 1)(e)
          val sid = output(depth + 1)(s)
          myid
        }

        case Assign(x, e) => {
          printNode(depth, myid, "Assign")
          val xid = output(depth + 1)(x)
          val eid = output(depth + 1)(e)
          myid
        }

        case Call(x, e1, e2, e3) => {
          printNode(depth, myid, "Call")
          val xid = output(depth + 1)(x)
          val e1id = output(depth + 1)(e1)
          val e2id = output(depth + 1)(e2)
          val e3id = output(depth + 1)(e3)
          myid
        }

        case New(x, e1, e2) => {
          printNode(depth, myid, "New")
          val xid = output(depth + 1)(x)
          val e1id = output(depth + 1)(e1)
          val e2id = output(depth + 1)(e2)
          myid
        }

        case Newfun(x, m, n) => {
          printNode(depth, myid, "Newfun")
          val xid = output(depth + 1)(x)
          val mid = output(depth + 1)(m)
          val nid = output(depth + 1)(n)
          myid
        }

        case ToObj(x, e) => {
          printNode(depth, myid, "ToObj")
          val xid = output(depth + 1)(x)
          val eid = output(depth + 1)(e)
          myid
        }

        case Del(x, e1, e2) => {
          printNode(depth, myid, "Del")
          val xid = output(depth + 1)(x)
          val e1id = output(depth + 1)(e1)
          val e2id = output(depth + 1)(e2)
          myid
        }

        case Update(e1, e2, e3) => {
          printNode(depth, myid, "Update")
          val e1id = output(depth + 1)(e1)
          val e2id = output(depth + 1)(e2)
          val e3id = output(depth + 1)(e3)
          myid
        }

        case Throw(e) => {
          printNode(depth, myid, "Throw")
          val eid = output(depth + 1)(e)
          myid
        }

        case Try(s1, x, s2, s3) => {
          printNode(depth, myid, "Try")
          val s1id = output(depth + 1)(s1)
          val xid = output(depth + 1)(x)
          val s2id = output(depth + 1)(s2)
          val s3id = output(depth + 1)(s3)
          myid
        }

        case Lbl(lbl, s) => {
          printNode(depth, myid, "Label: " + lbl)
          val sid = output(depth + 1)(s)
          myid
        }

        case Jump(lbl, e) => {
          printNode(depth, myid, "Jump: " + lbl)
          val eid = output(depth + 1)(e)
          myid
        }

        case For(x, e, s) => {
          printNode(depth, myid, "For")
          val xid = output(depth + 1)(x)
          val eid = output(depth + 1)(e)
          val sid = output(depth + 1)(s)
          myid
        }

        case Merge() => {
          printNode(depth, myid, "Merge")
          myid
        }

        case NumAST(v: Double) => {
          printNode(depth, myid, "Double: " + v.toString)
          myid
        }

        case BoolAST(v: Boolean) => {
          printNode(depth, myid, "Boolean: " + v.toString)
          myid
        }

        case StrAST(v: String) => {
          printNode(depth, myid, "String: " + v.toString)
          myid
        }

        case UndefAST() => {
          printNode(depth, myid, "Undef")
          myid
        }

        case NullAST() => {
          printNode(depth, myid, "Null")
          myid
        }

        case PVar(n) => {
          printNode(depth, myid, "Var: " + getStr(n) + " (" + n.toString + ")")
          myid
        }

        case Scratch(n) => {
          printNode(depth, myid, scratchText(n))
          myid
        }

        case Binop(op, e1, e2) => {
          printNode(depth, myid, "Binop: " + matchBop(op))
          val e1id = output(depth + 1)(e1)
          val e2id = output(depth + 1)(e2)
          myid
        }

        case Unop(op, e) => {
          printNode(depth, myid, "Unop: " + matchUop(op))
          val eid = output(depth + 1)(e)
          myid
        }

        case Method(self, args, s) => {
          printNode(depth, myid, "Method")
          val selfid = output(depth + 1)(self)
          val argsid = output(depth + 1)(args)
          val sid = output(depth + 1)(s)
          myid
        }

        case Print(e) => {
          printNode(depth, myid, "Print")
          val eid = output(depth + 1)(e)
          myid
        }

      }
    }
    //out.println("digraph AST {")
    output(0)(a)
    //out.println( "}" )
    out.close()
  }
}
