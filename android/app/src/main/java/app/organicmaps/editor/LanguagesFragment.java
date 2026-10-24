package app.organicmaps.editor;

import android.content.res.XmlResourceParser;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmRecyclerFragment;
import app.organicmaps.sdk.editor.Editor;
import app.organicmaps.sdk.editor.data.Language;
import app.organicmaps.sdk.util.log.Logger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LanguagesFragment extends BaseMwmRecyclerFragment<LanguagesAdapter>
{
  final static String EXISTING_LOCALIZED_NAMES = "ExistingLocalizedNames";
  // When set, the fragment lists the app's own languages instead of the map's name languages.
  public final static String APP_LOCALES = "AppLocales";

  private static final String TAG = LanguagesFragment.class.getSimpleName();
  private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

  public interface Listener
  {
    void onLanguageSelected(Language language);
  }

  private Listener mListener;

  @NonNull
  @Override
  protected LanguagesAdapter createAdapter()
  {
    Bundle args = getArguments();
    if (args != null && args.getBoolean(APP_LOCALES))
    {
      List<Language> appLanguages = getAppLanguages();
      return new LanguagesAdapter(this, appLanguages.toArray(new Language[appLanguages.size()]));
    }

    Set<String> existingLanguages =
        args != null ? new HashSet<>(args.getStringArrayList(EXISTING_LOCALIZED_NAMES)) : new HashSet<>();

    List<Language> languages = new ArrayList<>();
    for (Language lang : Editor.nativeGetSupportedLanguages(false))
    {
      if (existingLanguages.contains(lang.code))
        continue;

      languages.add(lang);
    }

    Collections.sort(languages, Comparator.comparing(lhs -> lhs.name));

    return new LanguagesAdapter(this, languages.toArray(new Language[languages.size()]));
  }

  // The app's own languages are the ones listed for per-app languages in the system settings.
  @NonNull
  private List<Language> getAppLanguages()
  {
    List<Language> languages = new ArrayList<>();
    XmlResourceParser parser = getResources().getXml(R.xml.locales_config);
    try
    {
      while (parser.next() != XmlPullParser.END_DOCUMENT)
      {
        if (parser.getEventType() != XmlPullParser.START_TAG || !"locale".equals(parser.getName()))
          continue;

        String code = parser.getAttributeValue(ANDROID_NS, "name");
        if (code == null)
          continue;

        // Each language is shown in itself, as the system picker does.
        Locale locale = Locale.forLanguageTag(code);
        languages.add(new Language(code, locale.getDisplayName(locale)));
      }
    }
    catch (IOException | XmlPullParserException e)
    {
      Logger.w(TAG, "Failed to read the supported app locales: " + e);
    }
    finally
    {
      parser.close();
    }

    Collections.sort(languages, Comparator.comparing(lhs -> lhs.name));
    return languages;
  }

  public void setListener(Listener listener)
  {
    this.mListener = listener;
  }

  protected void onLanguageSelected(Language language)
  {
    Fragment parent = getParentFragment();
    if (parent instanceof Listener)
      ((Listener) parent).onLanguageSelected(language);
    if (mListener != null)
      mListener.onLanguageSelected(language);
  }
}
