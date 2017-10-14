package xyz.klinker.messenger.shared.util

import android.util.Patterns

import java.util.regex.Pattern

object Regex {

    // region TLDs
    private val URL_VALID_GTLD = "(?:(?:" +
            "academy|accountants|active|actor|aero|agency|airforce|archi|army|arpa|asia|associates|attorney|audio|autos|" +
            "axa|bar|bargains|bayern|beer|berlin|best|bid|bike|bio|biz|black|blackfriday|blue|bmw|boutique|brussels|build|" +
            "builders|buzz|bzh|cab|camera|camp|cancerresearch|capetown|capital|cards|care|career|careers|cash|cat|catering|" +
            "center|ceo|cheap|christmas|church|citic|claims|cleaning|click|clinic|clothing|club|codes|coffee|college|cologne|com|" +
            "community|company|computer|condos|construction|consulting|contractors|cooking|cool|coop|country|credit|" +
            "creditcard|cruises|cuisinella|dance|dating|degree|democrat|dental|dentist|desi|diamonds|digital|direct|" +
            "directory|discount|dnp|domains|durban|edu|education|email|engineer|engineering|enterprises|equipment|estate|" +
            "eus|events|exchange|expert|exposed|fail|farm|feedback|finance|financial|fish|fishing|fitness|flights|florist|" +
            "foo|foundation|frogans|fund|furniture|futbol|gal|gallery|gift|gives|glass|global|globo|gmo|gop|google|gov|graphics|" +
            "gratis|green|gripe|guide|guitars|guru|hamburg|haus|hiphop|hiv|holdings|holiday|homes|horse|host|house|" +
            "immobilien|industries|info|ink|institute|insure|int|international|investments|ist|jetzt|jobs|joburg|juegos|kaufen|" +
            "kim|kitchen|kiwi|koeln|kred|land|lawyer|lease|legal|lgbt|life|lighting|limited|limo|link|loans|london|lotto|luxe|" +
            "luxury|maison|management|mango|market|marketing|media|meet|menu|miami|mil|mini|mobi|moda|moe|monash|mortgage|" +
            "moscow|motorcycles|museum|nagoya|name|navy|net|neustar|news|nhk|ninja|nyc|okinawa|onl|online|org|organic|ovh|paris|" +
            "partners|parts|photo|photography|photos|physio|pics|pictures|pink|place|plumbing|post|press|pro|productions|" +
            "properties|pub|qpon|quebec|recipes|red|rehab|reise|reisen|ren|rentals|repair|report|republican|rest|reviews|" +
            "rich|rio|rocks|rodeo|ruhr|ryukyu|saarland|schmidt|schule|scot|services|sexy|shiksha|shoes|singles|social|" +
            "software|sohu|solar|solutions|soy|space|spiegel|supplies|supply|support|surf|surgery|suzuki|systems|tattoo|" +
            "tax|tech|technology|tel|tienda|tips|tirol|today|tokyo|tools|town|toys|trade|training|travel|university|uno|" +
            "vacations|vegas|ventures|versicherung|vet|viajes|villas|vision|vlaanderen|vodka|vote|voting|voto|voyage|wang|" +
            "watch|webcam|website|wed|wien|wiki|works|wtc|wtf|xxx|xyz|yachts|yokohama|zone|дети|москва|онлайн|орг|сайт|" +
            "بازار|شبكة|موقع|संगठन|みんな|世界|中信|中文网|公司|公益|商城|商标|在线|我爱你|政务|机构|游戏|移动|组织机构|网址|网络|集团|삼성" +
            "ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bl|bm|bn|bo|bq|br|bs|bt|bv|" +
            "bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cu|cv|cw|cx|cy|cz|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|eu|" +
            "fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|" +
            "io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mf|" +
            "mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|" +
            "pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|ss|st|su|sv|sx|sy|" +
            "sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|um|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|" +
            "za|zm|zw|мкд|мон|рф|срб|укр|қаз|الاردن|الجزائر|السعودية|المغرب|امارات|ایران|بھارت|تونس|سودان|سورية|عمان|" +
            "فلسطين|قطر|مصر|مليسيا|پاکستان|भारत|বাংলা|ভারত|ਭਾਰਤ|ભારત|இந்தியா|இலங்கை|சிங்கப்பூர்|భారత్|ලංකා|ไทย|გე|中国|中國|台湾|" +
            "台灣|新加坡|香港|한국" +
            ")(?=[^\\p{Alnum}@]|$))"
    private val URL_VALID_CCTLD = "(?:(?:" +
            "ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bl|bm|bn|bo|bq|br|bs|bt|bv|" +
            "bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cu|cv|cw|cx|cy|cz|de|dj|dk|dm|do|dz|ec|ee|eg|eh|er|es|et|eu|" +
            "fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|" +
            "io|iq|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mf|" +
            "mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|" +
            "pk|pl|pm|pn|pr|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|ss|st|su|sv|sx|sy|" +
            "sz|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|tt|tv|tw|tz|ua|ug|uk|um|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|" +
            "za|zm|zw|мкд|мон|рф|срб|укр|қаз|الاردن|الجزائر|السعودية|المغرب|امارات|ایران|بھارت|تونس|سودان|سورية|عمان|" +
            "فلسطين|قطر|مصر|مليسيا|پاکستان|भारत|বাংলা|ভারত|ਭਾਰਤ|ભારત|இந்தியா|இலங்கை|சிங்கப்பூர்|భారత్|ලංකා|ไทย|გე|中国|中國|台湾|" +
            "台灣|新加坡|香港|한국" +
            ")(?=[^\\p{Alnum}@]|$))"
    // endregion
    // region subpatterns
    private val LATIN_ACCENTS_CHARS = "\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff" + // Latin-1

            "\\u0100-\\u024f" + // Latin Extended A and B

            "\\u0253\\u0254\\u0256\\u0257\\u0259\\u025b\\u0263\\u0268\\u026f\\u0272\\u0289\\u028b" + // IPA Extensions

            "\\u02bb" + // Hawaiian

            "\\u0300-\\u036f" + // Combining diacritics

            "\\u1e00-\\u1eff" // Latin Extended Additional (mostly for Vietnamese)

    private val URL_VALID_GENERAL_PATH_CHARS = "[a-z0-9!\\*';:=\\+,.\\$/%#\\[\\]\\-_~\\|&@$LATIN_ACCENTS_CHARS]"
    private val URL_BALANCED_PARENS = "\\(" +
            "(?:" +
            URL_VALID_GENERAL_PATH_CHARS + "+" +
            "|" +
            // allow one nested level of balanced parentheses
            "(?:" +
            URL_VALID_GENERAL_PATH_CHARS + "*" +
            "\\(" +
            URL_VALID_GENERAL_PATH_CHARS + "+" +
            "\\)" +
            URL_VALID_GENERAL_PATH_CHARS + "*" +
            ")" +
            ")" +
            "\\)"
    private val URL_VALID_PATH_ENDING_CHARS = "[a-z0-9=_#/\\-\\+$LATIN_ACCENTS_CHARS]|(?:$URL_BALANCED_PARENS)"

    private val URL_VALID_PATH = "(?:" +
            "(?:" +
            URL_VALID_GENERAL_PATH_CHARS + "*" +
            "(?:" + URL_BALANCED_PARENS + URL_VALID_GENERAL_PATH_CHARS + "*)*" +
            URL_VALID_PATH_ENDING_CHARS +
            ")|(?:@" + URL_VALID_GENERAL_PATH_CHARS + "+/)" +
            ")"

    private val URL_VALID_PORT_NUMBER = "[0-9]++"
    private val URL_VALID_CHARS = "[\\p{Alnum}$LATIN_ACCENTS_CHARS]"
    private val URL_VALID_SUBDOMAIN = "(?>(?:$URL_VALID_CHARS[$URL_VALID_CHARS\\-_]*)?$URL_VALID_CHARS\\.)"
    private val URL_VALID_DOMAIN_NAME = "(?:(?:$URL_VALID_CHARS[$URL_VALID_CHARS\\-]*)?$URL_VALID_CHARS\\.)"
    private val URL_VALID_UNICODE_CHARS = "[.[^\\p{Punct}\\s\\p{Z}\\p{InGeneralPunctuation}]]"
    private val URL_PUNYCODE = "(?:xn--[0-9a-z]+)"
    private val SPECIAL_URL_VALID_CCTLD = "(?:(?:" + "co|tv" + ")(?=[^\\p{Alnum}@]|$))"
    private val URL_VALID_URL_QUERY_CHARS = "[a-z0-9!?\\*'\\(\\);:&=\\+\\$/%#\\[\\]\\-_\\.,~\\|@]"
    private val URL_VALID_URL_QUERY_ENDING_CHARS = "[a-z0-9_&=#/]"

    private val URL_VALID_DOMAIN = "(?:" +                                                   // subdomains + domain + TLD

            URL_VALID_SUBDOMAIN + "+" + URL_VALID_DOMAIN_NAME +   // e.g. www.twitter.com, foo.co.jp, bar.co.uk

            "(?:" + URL_VALID_GTLD + "|" + URL_VALID_CCTLD + "|" + URL_PUNYCODE + ")" +
            ")" +
            "|(?:" +                                                  // domain + gTLD + some ccTLD

            URL_VALID_DOMAIN_NAME +                                 // e.g. twitter.com

            "(?:" + URL_VALID_GTLD + "|" + URL_PUNYCODE + "|" + SPECIAL_URL_VALID_CCTLD + ")" +
            ")" +
            "|(?:" + "(?<=https?://)" +
            "(?:" +
            "(?:" + URL_VALID_DOMAIN_NAME + URL_VALID_CCTLD + ")" +  // protocol + domain + ccTLD

            "|(?:" +
            URL_VALID_UNICODE_CHARS + "+\\." +                     // protocol + unicode domain + TLD

            "(?:" + URL_VALID_GTLD + "|" + URL_VALID_CCTLD + ")" +
            ")" +
            ")" +
            ")" +
            "|(?:" +                                                  // domain + ccTLD + '/'

            URL_VALID_DOMAIN_NAME + URL_VALID_CCTLD + "(?=/)" +     // e.g. t.co/

            ")"

    private val VALID_URL_PATTERN_STRING = "(" +                                                            //  $1 total match

            "(" +                                                          //  $3 URL

            "(https?://)?" +                                             //  $4 Protocol (optional)

            "(" + URL_VALID_DOMAIN + ")" +                               //  $5 Domain(s)

            "(?::(" + URL_VALID_PORT_NUMBER + "))?" +                     //  $6 Port number (optional)

            "(/" +
            URL_VALID_PATH + "*+" +
            ")?" +                                                       //  $7 URL Path and anchor

            "(\\?" + URL_VALID_URL_QUERY_CHARS + "*" +                   //  $8 Query String

            URL_VALID_URL_QUERY_ENDING_CHARS + ")?" +
            ")" +
            ")"
    // endregion

    val PHONE = Pattern.compile(
            "(\\+[0-9]+[\\- \\.]*)?"
                    + "(\\([0-9]+\\)[\\- \\.]*)?"
                    + "([0-9][0-9\\- \\.]+[0-9]{3,})")

    // Twitter's web regex
    val WEB_URL = Pattern.compile(VALID_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE)

    // Emojis
    val EMOJI = "[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]"
}
