# Notes: assumptions and tradeoffs

## Assumptions I made

- **No login, no user accounts.** I wasn't asked to build auth, so I made every report
  visible to anyone who can reach the app. In a real company deployment I'd obviously
  want to control who sees what — I'm calling that out as a known gap, not something I
  missed.
- **Mock data is fine, no real database needed.** I was told in-memory data was
  acceptable, so I loaded everything from JSON files into memory when the app starts.
  I don't save anything anywhere — if I restart the app, it comes back exactly the
  same. If I were turning this into a real product, my next step would be pointing it
  at an actual database instead, and I structured the code so that swap doesn't touch
  anything else (I explain how in the README's "Swapping the data source" section).
- **Exactly three reports, matching what was asked: Users, Departments, Projects.** I
  didn't invent extra reports, but I did add a couple of extra columns to the ones I
  was asked for (a department on the Users report, a budget on the Departments report)
  because they made the reports more useful without changing what was asked for.
- **The data is realistic but made up.** I generated synthetic names, emails, and
  numbers — there's no real person or company data anywhere. But I made sure it's
  internally consistent: every manager and every project owner I listed is a real
  person who actually exists elsewhere in the data, not a random unconnected name.

## Tradeoffs — things I chose on purpose, and why

**I built one table component for every report, instead of three separate tables.**
This was the biggest decision I made on the whole project. Instead of writing a "Users
table," a "Departments table," and a "Projects table," I built exactly one table
component, and I have the backend tell it what columns exist and how to treat each one
(is it a number? a date? sortable? filterable?). The upside: if I add a fourth report
later, it's one new file, not a new page. The downside: the code is a little more
abstract to read at first, because you won't find a literal `department.name`
reference anywhere in my table code — I drive all of it from the metadata instead.

**I made the URL the single source of truth for what you're looking at.** I store
every filter, sort, search term, and page number in the browser's address bar, not
hidden in the app's memory. That means you can copy a filtered URL, send it to someone
else, and they'll see exactly the same filtered view I saw. It also means the back
button works the way you'd expect. This is mostly invisible to a user — it's really a
decision I made about where state lives, specifically so the screen and the URL can
never quietly disagree with each other.

**I made search behave differently in two places, on purpose.** On the homepage, I
filter the report list instantly as you type, with no delay — because the list of
reports is small and already loaded, there's nothing to wait for. Inside a report, I
wait about a third of a second after you stop typing before I ask the server — because
that search has to fetch real data over the network, and I didn't want to fire a
request on every keystroke, which would be wasteful and would feel laggy, not faster.

**I send numbers and dates as raw values, not pre-formatted text.** My backend sends
`1250000`, not `"$1,250,000"`. I let the frontend decide how to display it. This is a
small thing but it matters to me: if I'd sent formatted strings from the backend,
changing the currency symbol or date format later would mean changing the backend, when
really that's a "how does this look" decision I think belongs on the frontend.

**I used pages, not infinite scroll.** I went with straightforward paging (Page 1, Page
2, ...) instead of loading more rows as you scroll. It's simpler for me to reason
about, easier to link to a specific page, and the report sizes I'm working with (12 to
120 rows) don't need anything fancier.

## What I deliberately didn't build, and why

- **Login / permissions** — I wasn't asked for this, and it would need a real user
  system to be meaningful, so I left it out rather than build something half-real.
- **Exporting to CSV** — this would be a genuinely small addition on top of what I
  already built (the same validated query, just written out as CSV instead of JSON),
  but I wasn't asked for it, so I didn't add it speculatively.
- **Remembering which columns you hid last time** — I don't have anywhere to store
  that without a login system, so I left it out.
- **A real database** — I built the code so this is a clean swap later (see the
  README's "Swapping the data source" section for exactly what I'd need to do), but
  actually building and testing a database adapter was more than I thought this
  exercise called for.

## Four real bugs I found while testing, not just assumed were fine

I want to be upfront about these, because I only found them by actually clicking
through the app myself, not just by reading my own code back:

1. Early on, I'd generated the row IDs in my sample data as text ("1", "2", "10")
   instead of numbers, which made the default row order look wrong — text sorts
   "1, 10, 2" instead of "1, 2, 10". I fixed it by making IDs real numbers.
2. When I requested a report ID that didn't exist, the "not found" message took a few
   seconds to appear instead of showing immediately, because my frontend's
   data-fetching library was quietly retrying a request I already knew would never
   succeed. I fixed it by telling that library not to retry that specific kind of
   failure.
3. I noticed dates were displaying one day earlier than they should have, depending on
   timezone — a classic date-parsing mistake I'd made. I fixed it in one shared place
   so I can't reintroduce it anywhere I add a new date display later.
4. I'd built the table header to stay pinned in place while you scroll down a long
   report, but I'd also put `overflow: hidden` on the box around the table (to keep its
   corners rounded), and I didn't realize that setting quietly cancels a pinned header —
   the browser needs that box to be the thing actually scrolling, and mine wasn't
   bounded in height, so the whole page scrolled past it instead. I only caught this by
   scrolling the actual page and measuring where the header ended up, not by reading
   the CSS and assuming it would work. I fixed it by giving that box a max height and
   letting it scroll internally, which is what "sticky" needed all along.
