# Scalable Product Discovery & External Integration Architecture

This document describes the Phase 4 Product Discovery Engine and External API Integration architecture implemented in ShopSmart AI 2.0.

## Overview & Target Flow

```
User Query
    ↓
Conversation Memory (ShoppingSessionContext)
    ↓
Shopping Intent / Query Parser (ParsedQuery)
    ↓
Product Discovery Service (ProductDiscoveryService)
    ↓
Multiple Product Providers (ProductProvider Interface)
    ├── FlipkartProductProvider (ProductSource.FLIPKART) ✅ IMPLEMENTED
    ├── LocalCatalogProvider (ProductSource.LOCAL)
    ├── ExternalProductApiProvider (ProductSource.PRODUCT_API)
    ├── Future Retail Provider A (ProductSource.RETAILER_A)
    └── Future Retail Provider B (ProductSource.RETAILER_B)
    ↓
Product Normalization & Metadata (ExternalProductNormalizer, FlipkartProductNormalizer)
    ↓
Deduplication & Multi-Offer Merging (ProductDeduplicationService & ProductOffer)
    ↓
Filtering (ProductFilterService)
    ↓
Ranking & Sorting (ProductRankingService)
    ↓
Pagination (PagedProductResult)
    ↓
Shopping Agent Orchestrator (ShoppingAgentService)
    ↓
Frontend API Response (/api/chat/recommend, /api/products/search)
```

---

## Core Components & Integration Modules

### 1. Provider Abstraction (`ProductProvider`)
- Strategy interface in `com.smartcart.ai.provider.ProductProvider`.
- Exposes `ProductSource getSource()` and `List<Product> discover(ProductDiscoveryQuery query)`.
- Implemented by `LocalCatalogProvider`, `ExternalProductApiProvider`, and `FlipkartProductProvider`.

### 2. Flipkart Affiliate API Integration (`FlipkartProductProvider`) ✅ NEW
- **Status:** Fully implemented in Phase 4 Step 3B
- **Purpose:** Provides real product data from India's largest e-commerce platform
- **Configuration:** Environment variable driven (`flipkart.provider.*`)
- **Graceful Fallback:** Returns empty results when credentials missing, allowing LocalCatalogProvider fallback

#### Flipkart Configuration Properties
- `FLIPKART_PROVIDER_ENABLED` (`false` by default)
- `FLIPKART_AFFILIATE_ID` (required when enabled)
- `FLIPKART_AFFILIATE_TOKEN` (required when enabled)
- `FLIPKART_API_BASE_URL` (default: `https://affiliate-api.flipkart.net/affiliate/api`)
- `FLIPKART_API_TIMEOUT` (default `5000` ms)

#### Flipkart Integration Components
- **FlipkartProviderConfig:** Configuration bean with validation
- **FlipkartApiClient:** HTTP client with Flipkart-specific authentication (Fk-Affiliate-Id, Fk-Affiliate-Token headers)
- **FlipkartProductNormalizer:** Converts Flipkart API responses to canonical Product entities
- **FlipkartProductProvider:** ProductProvider implementation with graceful error handling

### 3. External Provider Configuration (`ExternalProductProviderConfig`)
- Environment variable driven properties (`product.provider.external.*`):
  - `PRODUCT_PROVIDER_ENABLED` (`false` by default)
  - `PRODUCT_PROVIDER_BASE_URL`
  - `PRODUCT_PROVIDER_API_KEY`
  - `PRODUCT_PROVIDER_TIMEOUT` (default `5000` ms)
  - `PRODUCT_PROVIDER_PAGE_SIZE` (default `20`)
- Application functions seamlessly out-of-the-box using `LocalCatalogProvider` when external credentials are absent.

### 4. Generic External API Client (`ExternalProductApiClient`)
- Time-bounded, fault-tolerant HTTP client using `RestTemplate`.
- Sanitizes logs to prevent sensitive API keys or credentials from leaking.

### 5. Normalization Layer (`ExternalProductNormalizer`, `FlipkartProductNormalizer`)
- Maps dynamic external API responses (JSON maps/DTOs) into the canonical `Product` entity.
- Handles missing or null fields gracefully, setting default fallbacks (e.g. `currency="INR"`, `availability=true`).
- Prevents corrupt external payloads from crashing the shopping agent.

### 6. Multi-Offer Architecture (`ProductOffer`)
- `Product` entities maintain an `offers` array (`List<ProductOffer>`).
- When `ProductDeduplicationService` merges identical products from multiple providers (e.g. Local vs Flipkart vs External API), seller details, prices, and links are preserved in `Product.offers`.

### 7. Failure Isolation & Graceful Degradation
- Each provider call in `ProductDiscoveryService` is wrapped in exception handlers.
- If an external provider times out or returns HTTP 5xx errors, the failure is logged, and the agent falls back to other available providers without failing `POST /api/chat/recommend`.
- **Application never crashes due to missing credentials or API failures.**

### 8. Caching & Rate-Limit Readiness
- Boundaries isolate client execution from recommendation logic.
- Cache key boundaries based on `ProductDiscoveryQuery` allow future `@Cacheable` or Redis integration without altering core services.

---

## Environment Configuration & Key Safety

Example `.env` configuration (see `.env.example`):

```env
PORT=8080

# Flipkart Affiliate API (Primary Provider for India)
FLIPKART_PROVIDER_ENABLED=true
FLIPKART_AFFILIATE_ID=your_flipkart_affiliate_id
FLIPKART_AFFILIATE_TOKEN=your_flipkart_api_token
FLIPKART_API_BASE_URL=https://affiliate-api.flipkart.net/affiliate/api
FLIPKART_API_TIMEOUT=5000

# External Product Provider Configuration (Optional)
PRODUCT_PROVIDER_ENABLED=false
PRODUCT_PROVIDER_BASE_URL=https://api.example-retailer.com/v1
PRODUCT_PROVIDER_API_KEY=your_secret_key_here
PRODUCT_PROVIDER_TIMEOUT=5000
PRODUCT_PROVIDER_PAGE_SIZE=20
```

- API keys are handled **backend-only** and never exposed to the frontend.
- Running without API keys operates cleanly using the in-memory local catalog.
- **Never commit real credentials to version control.**

---

## Obtaining Flipkart Affiliate Credentials

To enable live Flipkart product data:

1. **Register as Flipkart Affiliate:**
   - Visit https://affiliate.flipkart.com/registerme
   - Complete registration and wait for approval

2. **Generate API Token:**
   - Login to https://affiliate.flipkart.com
   - Navigate to API → API Token
   - Click "Generate API Token"
   - Copy your Affiliate Tracking ID and API Token

3. **Configure Environment:**
   ```bash
   FLIPKART_PROVIDER_ENABLED=true
   FLIPKART_AFFILIATE_ID=your_affiliate_tracking_id
   FLIPKART_AFFILIATE_TOKEN=your_api_token
   ```

4. **Restart Application:**
   ```bash
   mvn spring-boot:run
   ```

---

## Testing Strategy

### Unit Tests
- `FlipkartProviderConfigTest`: Configuration validation
- `FlipkartProductNormalizerTest`: Product normalization with various field combinations
- `FlipkartProductProviderTest`: Provider logic with mocked API client

### Integration Tests
- `FlipkartProviderIntegrationTest`: Multi-provider aggregation, graceful fallback, deduplication

### Test Coverage
- ✅ Missing credentials handling
- ✅ API failure/timeout graceful fallback
- ✅ Malformed response handling
- ✅ Multi-provider aggregation
- ✅ Product deduplication across providers
- ✅ Source metadata preservation
- ✅ All existing 56 tests continue passing

### Running Tests
```bash
mvn test
```

Expected result: **All tests pass** (existing + new Flipkart tests)

---

## Security Considerations

### Credentials Protection
- ✅ No API keys in source code
- ✅ No credentials in test files
- ✅ Environment variable based configuration
- ✅ Sanitized logging (credentials redacted)
- ✅ .gitignore protects .env files

### Safe Practices
- API tokens never logged
- Authorization headers sanitized in logs
- No credentials in error messages exposed to users
- Configuration validation prevents accidental exposure

---

## Observability & Logging

### Provider Logs
```
FlipkartProductProvider: provider not configured or disabled — returning empty result
FlipkartProductProvider: discovering products for query='laptop', category='null'
FlipkartProductProvider: successfully normalized 5 products from Flipkart API
FlipkartProductProvider: error during product discovery — Network timeout. Returning empty result.
ProductDiscoveryService: aggregated 15 total raw products from 2 providers
```

### Configuration Logs
```
FlipkartProviderConfig: enabled=true but credentials are missing or incomplete
```

### Never Logged
- API tokens
- Authorization headers
- Sensitive user information

---

## Architecture Benefits

1. **Seamless Fallback:** Application works perfectly without external API credentials
2. **Multi-Provider Support:** Easy to add new providers (Amazon, eBay, Shopify, etc.)
3. **Fault Isolation:** One provider failure doesn't crash the application
4. **Clean Separation:** Provider logic isolated from business logic
5. **Extensible:** New providers implement ProductProvider interface
6. **Testable:** Comprehensive test coverage with mocks
7. **Production Ready:** Graceful degradation and error handling

---

## Future Enhancements

### Planned Providers
- Amazon Creators API (global coverage with India support)
- Shopify Global Catalog API (billions of products)
- eBay Browse API (auction/marketplace products)
- Additional India-focused retailers

### Future Features
- Redis caching for API responses
- Rate limiting per provider
- Provider health monitoring
- Dynamic provider prioritization
- A/B testing across providers
