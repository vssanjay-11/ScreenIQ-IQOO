import re

class EntityType:
    DATE_TIME = "DATE_TIME"
    LOCATION_ADDRESS = "LOCATION_ADDRESS"
    PHONE_NUMBER = "PHONE_NUMBER"
    EMAIL = "EMAIL"
    URL = "URL"
    PRODUCT_INFO = "PRODUCT_INFO"
    TASK_TODO = "TASK_TODO"
    DOCUMENT_SNIPPET = "DOCUMENT_SNIPPET"
    QR_BARCODE = "QR_BARCODE"
    NUMERIC_FINANCIAL = "NUMERIC_FINANCIAL"
    UNKNOWN = "UNKNOWN"

class DetectedEntity:
    def __init__(self, type, raw_value, normalized_value=None, confidence=1.0, metadata=None):
        self.type = type
        self.raw_value = raw_value
        self.normalized_value = normalized_value
        self.confidence = confidence
        self.metadata = metadata or {}

    def __repr__(self):
        return f"DetectedEntity(type={self.type}, raw={self.raw_value}, norm={self.normalized_value}, meta={self.metadata})"

class EntityPatternExtractor:
    PHONE_PATTERN = re.compile(r'(?:\+?\d{1,3}[-.\s]?)?(?:\(?\d{2,4}\)?[-.\s]?)?\d{3,5}[-.\s]?\d{4,5}\b')
    EMAIL_PATTERN = re.compile(r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\b')
    URL_PATTERN = re.compile(r'\b(?:https?://|www\.)[a-zA-Z0-9\-._~:/?#[\]@!$&\'()*+,;=]+|(?:[a-zA-Z0-9\-]+\.)+(?:com|org|net|io|edu|gov|in|ai|co|app|dev)(?:/[a-zA-Z0-9\-._~:/?#[\]@!$&\'()*+,;=]*)?\b', re.IGNORECASE)
    CURRENCY_PATTERN = re.compile(r'(?:\$|₹|€|£|Rs\.?|INR|USD|EUR)\s?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\b')
    DATE_PATTERN = re.compile(r'\b(?:\d{1,2}(?:st|nd|rd|th)?[\s/-](?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?|\d{1,2})[\s/-]\d{2,4}|(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?(?:,?\s+\d{2,4})?|\d{4}-\d{2}-\d{2}|today|tomorrow|yesterday)\b', re.IGNORECASE)
    TIME_PATTERN = re.compile(r'\b(?:(?:1[0-2]|0?[1-9]):[0-5][0-9]\s*(?:AM|PM|am|pm)?|(?:2[0-3]|[01]?[0-9]):[0-5][0-9]|(?:1[0-2]|0?[1-9])\s*(?:AM|PM|am|pm))\b')

    LOCATION_KEYWORDS = [
        "hall", "auditorium", "seminar", "campus", "university", "institute",
        "college", "road", "street", "st.", "avenue", "ave.", "block", "floor",
        "nagar", "colony", "layout", "sector", "chennai", "bangalore", "delhi", "mumbai",
        "room", "building", "complex", "lane", "junction"
    ]

    EVENT_KEYWORDS = [
        "workshop", "webinar", "conference", "hackathon", "meetup", "summit",
        "symposium", "seminar", "orientation", "masterclass", "keynote", "ceremony"
    ]

    def extract_entities(self, text):
        if not text.strip():
            return []
        entities = []

        # 1. Phone
        for m in self.PHONE_PATTERN.finditer(text):
            raw = m.group()
            clean = re.sub(r'[^\d+]', '', raw)
            if 7 <= len(clean.replace('+', '')) <= 15 and '/' not in raw:
                entities.append(DetectedEntity(EntityType.PHONE_NUMBER, raw.strip(), clean, 0.95))

        # 2. Email
        for m in self.EMAIL_PATTERN.finditer(text):
            raw = m.group()
            entities.append(DetectedEntity(EntityType.EMAIL, raw.strip(), raw.strip().lower(), 0.99))

        # 3. URL
        for m in self.URL_PATTERN.finditer(text):
            raw = m.group()
            if '@' not in raw:
                norm = raw.strip()
                if not norm.lower().startswith('http://') and not norm.lower().startswith('https://'):
                    norm = f"https://{norm}"
                entities.append(DetectedEntity(EntityType.URL, raw.strip(), norm, 0.95))

        # 4. Currency
        for m in self.CURRENCY_PATTERN.finditer(text):
            raw = m.group()
            norm = raw.strip().replace("Rs.", "₹").replace("Rs", "₹").replace("INR", "₹").replace("USD", "$").replace("EUR", "€")
            entities.append(DetectedEntity(EntityType.NUMERIC_FINANCIAL, raw.strip(), norm, 0.90))

        # 5. Dates
        for m in self.DATE_PATTERN.finditer(text):
            raw = m.group()
            entities.append(DetectedEntity(EntityType.DATE_TIME, raw.strip(), raw.strip(), 0.92, {"subType": "DATE"}))

        # 6. Times
        for m in self.TIME_PATTERN.finditer(text):
            raw = m.group()
            if ":" in raw or "am" in raw.lower() or "pm" in raw.lower():
                entities.append(DetectedEntity(EntityType.DATE_TIME, raw.strip(), raw.strip().upper(), 0.90, {"subType": "TIME"}))

        # 7. Addresses
        for line in text.splitlines():
            line_str = line.strip()
            if len(line_str) < 5:
                continue
            lower = line_str.lower()
            matches = [kw for kw in self.LOCATION_KEYWORDS if kw in lower]
            if len(matches) >= 1:
                if not self.EMAIL_PATTERN.search(line_str) and not self.URL_PATTERN.search(line_str):
                    entities.append(DetectedEntity(
                        EntityType.LOCATION_ADDRESS,
                        line_str,
                        line_str,
                        0.92 if len(matches) >= 2 else 0.82,
                        {"keywordMatches": str(len(matches))}
                    ))

        # 8. Event Clues
        for line in text.splitlines():
            line_str = line.strip()
            if len(line_str) < 4:
                continue
            lower = line_str.lower()
            match = next((kw for kw in self.EVENT_KEYWORDS if kw in lower), None)
            if match:
                entities.append(DetectedEntity(
                    EntityType.PRODUCT_INFO,
                    line_str,
                    line_str,
                    0.88,
                    {"clue": "EVENT_TOPIC", "keyword": match}
                ))

        # Unique by (type, raw_value)
        seen = set()
        unique = []
        for e in entities:
            key = (e.type, e.raw_value.lower())
            if key not in seen:
                seen.add(key)
                unique.append(e)
        return unique

def run_tests():
    extractor = EntityPatternExtractor()
    passed = 0
    total = 0

    def check(name, cond, msg=""):
        nonlocal passed, total
        total += 1
        if cond:
            passed += 1
            print(f" [PASS] {name}")
        else:
            print(f" [FAIL] {name}: {msg}")

    # TEST 1: Event Poster
    event_text = """AI Workshop
20 September 2026
10:00 AM
Seminar Hall 2, Anna University"""
    entities = extractor.extract_entities(event_text)
    check("Event Poster: Extracts Date", any(e.type == EntityType.DATE_TIME and "20 September" in e.raw_value for e in entities))
    check("Event Poster: Extracts Time", any(e.type == EntityType.DATE_TIME and "10:00 AM" in e.raw_value for e in entities))
    check("Event Poster: Extracts Venue/Address", any(e.type == EntityType.LOCATION_ADDRESS and "Seminar Hall 2" in e.raw_value for e in entities))
    check("Event Poster: Identifies Event Clue", any(e.metadata.get("clue") == "EVENT_TOPIC" or "AI Workshop" in e.raw_value for e in entities))

    # TEST 2: Product Listing
    prod_text = """Sony WH-1000XM5 Wireless Headphones
Limited Deal Price: ₹24,990
Original: ₹29,990 (17% off)
Free delivery by Tomorrow"""
    entities = extractor.extract_entities(prod_text)
    check("Product Listing: Extracts Currency/Price", any(e.type == EntityType.NUMERIC_FINANCIAL and "₹24,990" in e.raw_value for e in entities))
    check("Product Listing: Extracts Relative Date (Tomorrow)", any(e.type == EntityType.DATE_TIME and "Tomorrow" in e.raw_value for e in entities))

    # TEST 3: Phone
    phone_text = "Contact Support at +91 98765 43210 or alternate (800) 555-0199 for help."
    entities = extractor.extract_entities(phone_text)
    check("Phone: Extracts +91 phone", any(e.type == EntityType.PHONE_NUMBER and "98765" in e.raw_value for e in entities))
    check("Phone: Extracts (800) phone", any(e.type == EntityType.PHONE_NUMBER and "555-0199" in e.raw_value for e in entities))

    # TEST 4: URL
    url_text = "Visit https://screeniq.ai/demo or check docs.screeniq.io for setup."
    entities = extractor.extract_entities(url_text)
    check("URL: Extracts https link", any(e.type == EntityType.URL and e.normalized_value.startswith("https://screeniq.ai") for e in entities))
    check("URL: Normalizes domain without scheme", any(e.type == EntityType.URL and "docs.screeniq.io" in e.normalized_value for e in entities))

    # TEST 5: Email
    email_text = "Please reach out to support@screeniq.dev or team.lead+hackathon@iqoo.com"
    entities = extractor.extract_entities(email_text)
    check("Email: Standard email extracted", any(e.type == EntityType.EMAIL and e.raw_value == "support@screeniq.dev" for e in entities))
    check("Email: Tagged/sub-domain email extracted", any(e.type == EntityType.EMAIL and e.raw_value == "team.lead+hackathon@iqoo.com" for e in entities))

    # TEST 6: Address & Venue
    addr_text = """Office: Tech Park, 4th Floor, Sector 5, Outer Ring Road, Bangalore
PIN: 560103"""
    entities = extractor.extract_entities(addr_text)
    check("Address: Extracts street/road location line", any(e.type == EntityType.LOCATION_ADDRESS and "Ring Road" in e.raw_value for e in entities))

    # TEST 7: Plain Text
    plain_text = "The quick brown fox jumps over the lazy dog. Just a simple sentence."
    entities = extractor.extract_entities(plain_text)
    check("Plain Text: No false entities generated", len(entities) == 0, f"Found unexpected: {entities}")

    # TEST 8: Empty Text
    empty_text = "   "
    entities = extractor.extract_entities(empty_text)
    check("Empty Text: No entities generated", len(entities) == 0)

    print(f"\n=======================================================\nSUMMARY: {passed} / {total} tests passed\n=======================================================")
    assert passed == total

if __name__ == "__main__":
    run_tests()
