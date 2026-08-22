# Phase 4 Step 6 — Real-Time Product Discovery Foundation

## Status

Complete. ShopSmart remains fully usable with the local catalog when no external provider credentials are configured.

## Architecture

`ProductDiscoveryService` aggregates registered `ProductProvider` implementations, isolates provider failures, deduplicates candidates, applies shared filters, and delegates final ranking to `ProductRankingService` / `ProductScoringService`.

Current providers:

- `LocalCatalogProvider` — always available development catalog.
- `FlipkartProductProvider` — optional Affiliate API provider, disabled unless configured.
- `ExternalProductApiProvider` — optional generic permitted product API integration.
- `AmazonProviderConfig` — configuration placeholder only; no Amazon provider or scraping implementation exists.

## Provider configuration and security

- Credentials are read only from optional environment-backed properties.
- `backend/.env.example` contains placeholders only.
- `.env` files are ignored by Git.
- Provider logs never print credential values.
- No scraping, browser automation, or synthetic marketplace product data is used.

## Resilience and discovery behavior

- Provider health tracks available, disabled, unconfigured, error, and rate-limited states.
- Per-provider failure isolation, HTTP timeouts, in-memory TTL caching, and basic request/cooldown protection are implemented.
- An external provider failure never stops local catalog results.
- Discovery metadata reports queried/succeeded/failed providers, sources, and pre/post-deduplication counts.

## Normalization and source transparency

External adapters normalize provider fields into the internal `Product` model, including provider source, source product ID, provider product URL, availability, currency, and fetch time when supplied.

Local catalog products are explicitly marked `LOCAL` and do not receive fabricated retailer URLs. The frontend displays the product source and exposes **View product** only for a valid HTTP(S) URL from a non-local source.

## Verification

- Backend: `mvn.cmd test`
- Frontend: `npm.cmd run lint`
- Frontend: `npm.cmd run build`

External marketplace credentials are optional and were not required for local operation.

## Known limitations

- No real external marketplace credentials are configured in this repository, so local catalog fallback is the active runtime path.
- Amazon remains a configuration placeholder until a legitimate Product Advertising API integration is supplied.
- The in-memory cache is intentionally replaceable with a distributed cache in a later phase.

## Future provider integrations

Implement a provider only with documented, permitted APIs and credentials, normalize into `Product`, register the provider, and preserve local fallback and metadata behavior.
