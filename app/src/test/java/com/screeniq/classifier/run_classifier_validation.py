"""
ScreenIQ Agent 5 Validation Suite
Direct Python Mirror of LayeredContentClassifier & Regex Logic
Validates the 9 required test cases:
1. EVENT
2. PRODUCT
3. LOCATION (ADDRESS)
4. PHONE
5. URL
6. CONTACT
7. TASK
8. DOCUMENT
9. UNKNOWN
"""

import re
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

# Regexes matching DeterministicExtractor.kt
EMAIL_REGEX = re.compile(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b')
URL_REGEX = re.compile(r'\b(?:https?://|www\.)[^\s/$.?#].[^\s]*\b|\b[a-zA-Z0-9-]+\.(?:com|org|net|io|edu|gov|in|co|ai)(?:/[^\s]*)?\b', re.IGNORECASE)
PHONE_REGEX = re.compile(r'(?:\+?\d{1,3}[-.\s]?)?(?:\(?\d{2,4}\)?[-.\s]?)?\d{3,5}[-.\s]?\d{4,5}\b')
DATE_REGEX = re.compile(r'\b(?:\d{1,2}(?:st|nd|rd|th)?\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)(?:\s+\d{4})?|(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?(?:\s*,\s*\d{4})?|\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}-\d{2}-\d{2})\b', re.IGNORECASE)
TIME_REGEX = re.compile(r'\b(?:\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?|\d{1,2}\s*(?:AM|PM|am|pm))\b')
PRICE_REGEX = re.compile(r'(?<!\w)(?:[₹$€£]|Rs\.?|INR|USD)\s*[\d,]+(?:\.\d{1,2})?\b|\b[\d,]+(?:\.\d{1,2})?\s*(?:INR|USD)\b', re.IGNORECASE)
TASK_MARKER_REGEX = re.compile(r'^\s*(?:\[[\sXx]?\]|TODO:|Task:|Action Item:|- \[ \])\s*(.*)$', re.IGNORECASE | re.MULTILINE)
PINCODE_REGEX = re.compile(r'\b[1-9][0-9]{5}\b')
QR_PAYLOAD_REGEX = re.compile(r'\b(?:upi://pay\?[^\s]+|WIFI:S:[^;]+;|MATMSG:[^;]+;)\b', re.IGNORECASE)

EVENT_KEYWORDS = {
    "workshop", "webinar", "conference", "meetup", "hackathon", "summit",
    "session", "seminar", "symposium", "festival", "concert", "exhibition",
    "ceremony", "rsvp", "register", "venue", "agenda", "speakers", "hall",
    "am", "pm", "september", "october", "november", "december", "january",
    "february", "march", "april", "may", "june", "july", "august"
}

PRODUCT_KEYWORDS = {
    "buy", "cart", "order", "price", "mrp", "discount", "off", "deal",
    "delivery", "stock", "warranty", "specs", "specification", "camera",
    "battery", "mah", "processor", "ram", "storage", "snapdragon", "display",
    "amazon", "flipkart", "iqoo", "smartphone", "laptop", "reviews", "rating"
}

TASK_KEYWORDS = {
    "todo", "task", "action item", "checklist", "deadline", "due date",
    "priority", "urgent", "assigned", "follow up", "submit before", "reminder"
}

CONTACT_KEYWORDS = {
    "dr.", "prof.", "mr.", "ms.", "mrs.", "ceo", "cto", "founder", "director",
    "manager", "engineer", "designer", "consultant", "officer", "executive",
    "contact person", "designation", "business card", "phone:", "mobile:", "tel:"
}

ADDRESS_KEYWORDS = [
    "street", "road", "rd", "st", "avenue", "ave", "nagar", "salai", "lane",
    "hall", "auditorium", "seminar hall", "campus", "university", "institute",
    "chennai", "bengaluru", "bangalore", "delhi", "mumbai", "hyderabad", "floor",
    "building", "block", "pincode", "pin:", "zip:", "opp", "near"
]

def classify_text(raw_text: str):
    trimmed = raw_text.strip()
    if not trimmed:
        return "UNKNOWN", 0.99, {}, "Screen text is blank."

    # Stage 1: Deterministic extraction
    entities = []
    for m in QR_PAYLOAD_REGEX.finditer(raw_text):
        entities.append(("QR_BARCODE", m.group()))
    for m in URL_REGEX.finditer(raw_text):
        entities.append(("URL", m.group()))
    for m in EMAIL_REGEX.finditer(raw_text):
        entities.append(("EMAIL", m.group()))
    for m in PHONE_REGEX.finditer(raw_text):
        digits = [c for c in m.group() if c.isdigit()]
        if 10 <= len(digits) <= 13:
            entities.append(("PHONE_NUMBER", m.group()))
    for m in DATE_REGEX.finditer(raw_text):
        entities.append(("DATE_TIME", m.group()))
    for m in TIME_REGEX.finditer(raw_text):
        entities.append(("DATE_TIME", m.group()))
    for m in PRICE_REGEX.finditer(raw_text):
        entities.append(("NUMERIC_FINANCIAL", m.group()))
    for m in TASK_MARKER_REGEX.finditer(raw_text):
        entities.append(("TASK_TODO", m.group(1) or m.group()))

    lines = [l.strip() for l in raw_text.splitlines() if l.strip()]
    for l in lines:
        lower_l = l.lower()
        if any(kw in lower_l for kw in ADDRESS_KEYWORDS) or PINCODE_REGEX.search(l):
            entities.append(("LOCATION_ADDRESS", l))

    words = [w.lower() for w in re.split(r'\s+', trimmed) if w]
    word_count = len(words)
    entity_types = {t for t, _ in entities}

    # QR check
    if "QR_BARCODE" in entity_types:
        return "QR_CODE", 0.96, {"payload": [val for t, val in entities if t == "QR_BARCODE"][0]}, "High-confidence QR payload."

    # Isolated entities
    if word_count <= 8:
        has_phone = "PHONE_NUMBER" in entity_types
        has_email = "EMAIL" in entity_types
        has_url = "URL" in entity_types
        if has_phone and not has_email and not has_url and (word_count <= 4 or any(w in words for w in ["call", "phone", "dial", "tel", "mobile"])):
            phone_val = [val for t, val in entities if t == "PHONE_NUMBER"][0]
            return "PHONE", 0.95, {"phoneNumber": phone_val, "label": "Mobile"}, "Isolated phone number with call intent."
        if has_email and not has_phone and not has_url and word_count <= 4:
            email_val = [val for t, val in entities if t == "EMAIL"][0]
            return "EMAIL", 0.95, {"emailAddress": email_val, "domain": email_val.split("@")[-1]}, "Isolated email address."
        if has_url and not has_phone and not has_email and word_count <= 4:
            url_val = [val for t, val in entities if t == "URL"][0]
            return "URL", 0.95, {"url": url_val, "domain": url_val.replace("https://", "").replace("http://", "").split("/")[0]}, "Isolated web URL."

    # Multi-signal scoring
    scores = {}

    has_date = "DATE_TIME" in entity_types
    has_location = "LOCATION_ADDRESS" in entity_types
    event_hits = sum(1 for w in words if w in EVENT_KEYWORDS)
    event_score = 0.0
    if has_date and has_location: event_score += 0.50
    if has_date: event_score += 0.25
    if event_hits > 0: event_score += min(event_hits * 0.12, 0.40)
    if event_score > 0: scores["EVENT"] = event_score

    has_price = "NUMERIC_FINANCIAL" in entity_types
    product_hits = sum(1 for w in words if w in PRODUCT_KEYWORDS)
    prod_score = 0.0
    if has_price: prod_score += 0.45
    if product_hits > 0: prod_score += min(product_hits * 0.12, 0.45)
    if prod_score > 0: scores["PRODUCT"] = prod_score

    has_task_entity = "TASK_TODO" in entity_types
    task_hits = sum(1 for w in words if w in TASK_KEYWORDS)
    task_score = 0.0
    if has_task_entity: task_score += 0.55
    if task_hits > 0: task_score += min(task_hits * 0.15, 0.40)
    if task_score > 0: scores["TASK"] = task_score

    has_c_phone = "PHONE_NUMBER" in entity_types
    has_c_email = "EMAIL" in entity_types
    contact_hits = sum(1 for w in words if w in CONTACT_KEYWORDS)
    contact_score = 0.0
    if has_c_phone and has_c_email: contact_score += 0.45
    elif has_c_phone or has_c_email: contact_score += 0.20
    if contact_hits > 0: contact_score += min(contact_hits * 0.15, 0.45)
    if contact_score > 0: scores["CONTACT"] = contact_score

    if has_location and not has_date:
        loc_hits = sum(1 for w in words if w in ["street", "road", "nagar", "salai", "building", "chennai", "delhi", "mumbai"])
        scores["LOCATION"] = 0.50 + min(loc_hits * 0.10, 0.40)

    if word_count >= 35:
        scores["DOCUMENT"] = 0.45 + min(word_count / 150.0, 0.45)

    if not scores:
        if word_count > 3:
            return "TEXT", 0.70, {"snippet": raw_text[:120]}, "General text."
        else:
            return "UNKNOWN", 0.50, {"reason": "Insufficient semantic signal."}, "Unrecognized content."

    top_cat = max(scores.items(), key=lambda x: x[1])
    conf = min(max(top_cat[1], 0.55), 0.96)

    # Structured fields extraction
    fields = {}
    if top_cat[0] == "EVENT":
        title = next((l for l in lines if not re.search(r'(?i)(?:am|pm|\d{1,2}:\d{2}|hall|chennai|delhi)', l) and len(l) <= 60), lines[0])
        fields["title"] = title
        dates = [val for t, val in entities if t == "DATE_TIME" and not re.search(r'(?i)(?:am|pm|:\d{2})', val)]
        times = [val for t, val in entities if t == "DATE_TIME" and re.search(r'(?i)(?:am|pm|:\d{2})', val)]
        locs = [val for t, val in entities if t == "LOCATION_ADDRESS"]
        fields["date"] = dates[0] if dates else (entities[0][1] if dates else "")
        fields["time"] = times[0] if times else ""
        fields["location"] = locs[0] if locs else lines[-1]
    elif top_cat[0] == "PRODUCT":
        fields["title"] = lines[0]
        prices = [val for t, val in entities if t == "NUMERIC_FINANCIAL"]
        fields["price"] = prices[0] if prices else ""
        low_t = raw_text.lower()
        fields["merchant"] = "Amazon" if "amazon" in low_t else ("Flipkart" if "flipkart" in low_t else "Online Store")
    elif top_cat[0] == "LOCATION":
        fields["address"] = lines[0]
        pin = PINCODE_REGEX.search(raw_text)
        fields["postalCode"] = pin.group() if pin else ""
        cities = ["Chennai", "Bengaluru", "Mumbai", "Delhi", "Hyderabad"]
        fields["city"] = next((c for c in cities if c.lower() in raw_text.lower()), "")
    elif top_cat[0] == "CONTACT":
        fields["name"] = lines[0]
        phones = [val for t, val in entities if t == "PHONE_NUMBER"]
        emails = [val for t, val in entities if t == "EMAIL"]
        fields["phone"] = phones[0] if phones else ""
        fields["email"] = emails[0] if emails else ""
        org_line = next((l for l in lines if re.search(r'(?i)(?:director|engineer|manager|architect|lead|corp|ltd)', l)), "")
        fields["organization"] = org_line
    elif top_cat[0] == "TASK":
        tasks = [val for t, val in entities if t == "TASK_TODO"]
        fields["title"] = tasks[0] if tasks else lines[0]
        fields["priority"] = "HIGH" if re.search(r'(?i)(?:urgent|asap|priority)', raw_text) else "NORMAL"
    elif top_cat[0] == "DOCUMENT":
        fields["title"] = lines[0]
        fields["wordCount"] = str(word_count)
        fields["summarySnippet"] = " ".join(lines[:2])[:150]

    return top_cat[0], conf, fields, f"Classified as {top_cat[0]} based on score {top_cat[1]:.2f}"

def run_tests():
    test_cases = [
        (
            "EVENT",
            "AI Workshop\n20 September\n10 AM\nChennai",
            "EVENT",
            {"title": "AI Workshop", "date": "20 September", "time": "10 AM", "location": "Chennai"}
        ),
        (
            "PRODUCT",
            "iQOO 12 5G Smartphone\n₹52,999\nSnapdragon 8 Gen 3, 16GB RAM\nFree Delivery on Amazon",
            "PRODUCT",
            {"price": "₹52,999", "merchant": "Amazon"}
        ),
        (
            "LOCATION",
            "Anna University Highway Campus, Sardar Patel Road, Guindy, Chennai 600025",
            "LOCATION",
            {"city": "Chennai", "postalCode": "600025"}
        ),
        (
            "PHONE",
            "Call +91 98765 43210",
            "PHONE",
            {"phoneNumber": "+91 98765 43210"}
        ),
        (
            "URL",
            "https://github.com/iqoo/screen-iq",
            "URL",
            {"url": "https://github.com/iqoo/screen-iq", "domain": "github.com"}
        ),
        (
            "CONTACT",
            "Dr. Arvind Raman\nChief AI Architect\nTechCorp India\narvind@techcorp.in\n+91 98401 12345",
            "CONTACT",
            {"name": "Dr. Arvind Raman", "email": "arvind@techcorp.in"}
        ),
        (
            "TASK",
            "[ ] Prepare presentation slides for iQOO Hackathon demo by 20 Sept\nPriority: Urgent",
            "TASK",
            {"priority": "HIGH"}
        ),
        (
            "DOCUMENT",
            "Privacy Policy and Data Governance Guidelines\nThis document outlines the strict privacy-first architecture of the on-device AI system. All screen buffers are held strictly in volatile memory. No screen pixels are ever persisted to disk or transmitted to remote cloud servers without explicit user consent. Data retention is strictly local and user-controlled, ensuring compliance with hackathon data handling and phone-first privacy protocols.",
            "DOCUMENT",
            {"title": "Privacy Policy and Data Governance Guidelines"}
        ),
        (
            "UNKNOWN",
            "#@!$%^&* ???",
            "UNKNOWN",
            {}
        )
    ]

    all_passed = True
    print(f"{'#':<3} | {'TEST CASE':<12} | {'EXPECTED':<10} | {'ACTUAL':<10} | {'CONF':<6} | {'STATUS':<6}")
    print("-" * 65)

    for i, (name, text, expected_cat, expected_fields) in enumerate(test_cases, 1):
        cat, conf, fields, reason = classify_text(text)
        cat_match = (cat == expected_cat)
        fields_match = all(fields.get(k) == v or v in fields.get(k, "") for k, v in expected_fields.items())
        passed = cat_match and fields_match

        if not passed:
            all_passed = False

        status_str = "PASS" if passed else "FAIL"
        print(f"{i:<3} | {name:<12} | {expected_cat:<10} | {cat:<10} | {conf:<6.2f} | {status_str:<6}")
        if not passed:
            print(f"    Expected: {expected_cat}, fields: {expected_fields}")
            print(f"    Actual:   {cat}, fields: {fields}")
        else:
            print(f"    Structured fields: {fields}")

    print("-" * 65)
    if all_passed:
        print(">>> ALL 9 REQUIRED TEST SCENARIOS PASSED SUCCESSFULLY! <<<")
    else:
        print(">>> SOME TESTS FAILED! <<<")
        sys.exit(1)

if __name__ == '__main__':
    run_tests()
