package Util

/**
  * Created by yairf on 10/02/2018.
  */

import java.util

import Util.TextCleaner.cleanText
import com.google.common.base.Optional
import com.optimaize.langdetect.{LanguageDetector, LanguageDetectorBuilder}
import com.optimaize.langdetect.i18n.LdLocale
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.{LanguageProfile, LanguageProfileReader}
import com.optimaize.langdetect.text.{CommonTextObjectFactories, TextObject, TextObjectFactory}
import edu.stanford.nlp.simple.Sentence
import org.apache.spark.sql.functions.udf

class TextTools{}
object TextTools {

  def lemmatizeText(txt: String) = new Sentence(txt).lemmas().toArray().mkString(" ")

  //load all languages:
  val languageProfiles: util.List[LanguageProfile] = new LanguageProfileReader().readAllBuiltIn

  //build language detector:
  val languageDetector: LanguageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard).withProfiles(languageProfiles).build

  //create a text object factory
  val textObjectFactory: TextObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText



  val  isEnglish = udf { text: String => {
    //asciiEncoder.canEncode(text) //|| isoEncoder.canEncode(text)
    //query:
    val textObject: TextObject = textObjectFactory.forText(text)
    languageDetector.detect(textObject).equals(Optional.of(LdLocale.fromString("en")))
  }
  }

  val preProcessText = udf { txt: String =>{
      var text = cleanText(txt)
      if (text.length > 0) text = lemmatizeText(text).toString
      text
    }
  }
}
