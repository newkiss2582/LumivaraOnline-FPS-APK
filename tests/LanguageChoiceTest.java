import com.lumivaraonline.fps.LanguageChoice;

public class LanguageChoiceTest {
    private static void expect(String actual,String wanted){if(!wanted.equals(actual))throw new AssertionError(actual+" != "+wanted);}
    public static void main(String[] args){
        expect(LanguageChoice.normalize(null),"th");
        expect(LanguageChoice.normalize("unsupported"),"th");
        expect(LanguageChoice.resolve("th","en"),"th");
        expect(LanguageChoice.resolve("en","th"),"en");
        expect(LanguageChoice.resolve("system","th"),"th");
        expect(LanguageChoice.resolve("system","th-TH"),"th");
        expect(LanguageChoice.resolve("system","TH_th"),"th");
        expect(LanguageChoice.resolve("system","en-US"),"en");
        expect(LanguageChoice.resolve("system","zh"),"en");
        expect(LanguageChoice.resolve("system",null),"en");
        if(LanguageChoice.index("th")!=0||LanguageChoice.index("en")!=1||LanguageChoice.index("system")!=2)throw new AssertionError("Wrong picker selection");
        System.out.println("PASS: explicit language overrides device language, follow-device handles Thai variants and English fallback, invalid stored preferences fall back safely, picker selection indices.");
    }
}
