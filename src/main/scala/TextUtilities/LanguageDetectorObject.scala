package TextUtilities

import com.optimaize.langdetect
import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.{CommonTextObjectFactories, TextObjectFactory}

/**
  * Created by yairf on 12/02/2018.
  */
import com.optimaize.langdetect
import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.{CommonTextObjectFactories, TextObjectFactory}

/**
  * Alternative solution to LanguageDetector
  */
object LanguageDetectorObject {
  lazy val languageDetector: langdetect.LanguageDetector = {
    val languageProfiles = new LanguageProfileReader().readAllBuiltIn
    LanguageDetectorBuilder.create(NgramExtractors.standard).withProfiles(languageProfiles).build
  }

  lazy val textObjectFactory: TextObjectFactory = CommonTextObjectFactories.forDetectingShortCleanText

  def detect(text: String): Option[String] = {
    val textObject = textObjectFactory.forText(text)
    val lang = languageDetector.detect(textObject)
    if (lang.isPresent)
      Some(lang.get.getLanguage)
    else
      None
  }
}

