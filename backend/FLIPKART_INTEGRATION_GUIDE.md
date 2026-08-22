# Flipkart Affiliate API Integration Guide

## Overview

ShopSmart AI 2.0 now includes **Flipkart Affiliate API** integration as the primary real product data source for Indian users. This guide explains how to enable, configure, and test the Flipkart provider.

---

## Architecture Summary

```
User Query
    ↓
Shopping Agent (ShoppingAgentService)
    ↓
Product Discovery Service
    ↓
Multiple Product Providers
    ├── FlipkartProductProvider (ProductSource.FLIPKART) ✅ NEW
    ├── LocalCatalogProvider (ProductSource.LOCAL)
    └── Future providers...
    ↓
Normalization & Deduplication
    ↓
Filtering & Ranking
    ↓
Paginated Results
    ↓
API Response
```

### Key Components

1. **FlipkartProviderConfig**: Environment-based configuration with validation
2. **FlipkartApiClient**: HTTP client with Flipkart-specific authentication
3. **FlipkartProductNormalizer**: Converts Flipkart API responses to Product entities
4. **FlipkartProductProvider**: ProductProvider implementation with graceful error handling

---

## Obtaining Flipkart Affiliate Credentials

### Step 1: Register as Flipkart Affiliate

1. Visit https://affiliate.flipkart.com/registerme
2. Fill in the registration form:
   - Your name and email
   - Website/App details (if applicable)
   - Traffic source information
3. Submit and wait for approval (typically 24-48 hours)

### Step 2: Generate API Token

1. Login to https://affiliate.flipkart.com
2. Navigate to **API → API Token**
3. Your **Affiliate Tracking ID** is displayed automatically
4. Click **Generate API Token**
5. Copy both:
   - **Affiliate Tracking ID** (e.g., `youraffiliateID`)
   - **Affiliate API Token** (e.g., `xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)

⚠️ **Important**: Only one token is allowed per affiliate account. Generating a new token disables the old one.

---

## Configuration

### Environment Variables

Create or update your `.env` file in the `backend/` directory:

```env
# Flipkart Affiliate API Configuration
FLIPKART_PROVIDER_ENABLED=true
FLIPKART_AFFILIATE_ID=your_affiliate_tracking_id
FLIPKART_AFFILIATE_TOKEN=your_api_token
FLIPKART_API_BASE_URL=https://affiliate-api.flipkart.net/affiliate/api
FLIPKART_API_TIMEOUT=5000
```

### Configuration Properties

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `FLIPKART_PROVIDER_ENABLED` | No | `false` | Enable/disable Flipkart provider |
| `FLIPKART_AFFILIATE_ID` | Yes* | - | Your Flipkart Affiliate Tracking ID |
| `FLIPKART_AFFILIATE_TOKEN` | Yes* | - | Your Flipkart Affiliate API Token |
| `FLIPKART_API_BASE_URL` | No | `https://affiliate-api.flipkart.net/affiliate/api` | Flipkart API base URL |
| `FLIPKART_API_TIMEOUT` | No | `5000` | Request timeout in milliseconds |

*Required when `FLIPKART_PROVIDER_ENABLED=true`

---

## Running the Application

### With Flipkart Enabled

```bash
cd backend

# Set environment variables (or use .env file)
export FLIPKART_PROVIDER_ENABLED=true
export FLIPKART_AFFILIATE_ID=your_affiliate_id
export FLIPKART_AFFILIATE_TOKEN=your_token

# Run application
mvn spring-boot:run
```

### Without Flipkart (Local Catalog Only)

```bash
cd backend

# Flipkart disabled by default
mvn spring-boot:run
```

The application **works perfectly without Flipkart credentials**, using the LocalCatalogProvider as fallback.

---

## Verifying Integration

### Check Application Logs

On startup, look for:

```
✅ FlipkartProviderConfig: Flipkart provider fully configured and enabled
```

Or if credentials are missing:

```
⚠️ FlipkartProviderConfig: enabled=true but credentials are missing or incomplete.
   Flipkart provider will be disabled. Set FLIPKART_AFFILIATE_ID and FLIPKART_AFFILIATE_TOKEN.
```

### Test API Endpoint

```bash
# Test product search
curl -X POST http://localhost:8080/api/chat/recommend \
  -H "Content-Type: application/json" \
  -d '{"message": "laptop under 60000"}'
```

Check the response for products with `"source": "FLIPKART"`:

```json
{
  "message": "I found 5 laptops under ₹60,000...",
  "products": [
    {
      "id": "FLP12345",
      "name": "Lenovo ThinkPad E14 Gen 5",
      "brand": "Lenovo",
      "price": 58000,
      "source": "FLIPKART",
      "currency": "INR",
      "storeName": "Flipkart",
      "productUrl": "https://www.flipkart.com/...",
      ...
    }
  ]
}
```

---

## Graceful Fallback Behavior

The Flipkart integration is designed for **zero-downtime** operation:

### Scenario 1: Credentials Not Configured
- **Behavior**: FlipkartProvider returns empty results
- **Fallback**: LocalCatalogProvider provides products
- **User Impact**: None (users get local catalog products)

### Scenario 2: Flipkart API Unavailable
- **Behavior**: FlipkartApiClient times out after 5 seconds
- **Fallback**: LocalCatalogProvider provides products
- **User Impact**: Slight delay (5 seconds), then local products

### Scenario 3: Flipkart API Error
- **Behavior**: FlipkartProvider catches exception and logs error
- **Fallback**: LocalCatalogProvider provides products
- **User Impact**: None (error logged server-side only)

### Scenario 4: Malformed Response
- **Behavior**: FlipkartProductNormalizer filters invalid products
- **Fallback**: Valid products returned, invalid ones skipped
- **User Impact**: None (only valid products shown)

---

## Multi-Provider Aggregation

When multiple providers are enabled, ShopSmart AI aggregates results:

```
ProductDiscoveryService
    ↓
Calls all enabled providers in parallel
    ├── FlipkartProvider → 10 products
    └── LocalCatalogProvider → 15 products
    ↓
Deduplication (same product from multiple sources)
    ↓
Filtering (category, price, brand, rating)
    ↓
Ranking (relevance scoring)
    ↓
Pagination (page 1, page size 5)
    ↓
Final Result: 5 best products
```

### Product Deduplication

Products are deduplicated by:
1. Normalized product name (case-insensitive, trimmed)
2. Brand match
3. Category match

When duplicates are found, the product with the **higher rating** is kept.

---

## Testing

### Run All Tests

```bash
cd backend
mvn test
```

Expected output:
```
Tests run: 96, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Flipkart-Specific Tests

```bash
# Test Flipkart normalizer
mvn test -Dtest=FlipkartProductNormalizerTest

# Test Flipkart provider
mvn test -Dtest=FlipkartProductProviderTest

# Test multi-provider integration
mvn test -Dtest=FlipkartProviderIntegrationTest

# Test configuration
mvn test -Dtest=FlipkartProviderConfigTest
```

---

## Security Best Practices

### ✅ DO

- ✅ Store credentials in environment variables or `.env` file
- ✅ Add `.env` to `.gitignore`
- ✅ Use different credentials for development and production
- ✅ Rotate API tokens periodically
- ✅ Monitor API usage in Flipkart dashboard

### ❌ DON'T

- ❌ Hardcode credentials in source code
- ❌ Commit credentials to Git
- ❌ Share API tokens publicly
- ❌ Log API tokens in application logs
- ❌ Expose credentials in frontend code

### Credential Protection Features

1. **No credentials in source code**: All configuration via environment variables
2. **Sanitized logging**: FlipkartApiClient redacts credentials from logs
3. **No client-side exposure**: Credentials stay on backend only
4. **Graceful degradation**: Missing credentials don't crash the application

---

## Troubleshooting

### Issue: "Flipkart provider disabled — credentials unavailable"

**Solution**: Set environment variables:
```bash
export FLIPKART_PROVIDER_ENABLED=true
export FLIPKART_AFFILIATE_ID=your_id
export FLIPKART_AFFILIATE_TOKEN=your_token
```

### Issue: "FlipkartApiClient: request failed — Network timeout"

**Possible causes**:
1. Flipkart API is temporarily unavailable
2. Network connectivity issues
3. Invalid base URL

**Solution**: Check logs for detailed error. Application will fallback to LocalCatalogProvider.

### Issue: "No products found in Flipkart API response"

**Possible causes**:
1. Invalid credentials (401 Unauthorized)
2. API rate limit exceeded (429 Too Many Requests)
3. No products match the search query

**Solution**: 
- Verify credentials in Flipkart dashboard
- Check API usage limits
- Try a different search query

### Issue: Products not showing Flipkart source

**Check**:
1. Is `FLIPKART_PROVIDER_ENABLED=true`?
2. Are credentials correctly set?
3. Check application logs for provider status

---

## API Endpoint Mapping

Flipkart API endpoints used:

| Endpoint | Purpose | Query Parameters |
|----------|---------|-----------------|
| `/products/search` | Keyword search | `query`, `page`, `resultCount` |
| `/products/category` | Category browse | `category`, `brand`, `minPrice`, `maxPrice` |

ShopSmart AI automatically selects the appropriate endpoint based on the user query.

---

## Product Data Mapping

Flipkart API fields → ShopSmart Product entity:

| Flipkart Field | Product Entity Field | Notes |
|----------------|---------------------|-------|
| `productId` | `id` | Primary identifier |
| `title` | `name` | Product name |
| `brand` | `brand` | Brand name |
| `category` | `category` | Normalized to lowercase |
| `sellingPrice` / `price` | `price` | In INR |
| `rating` | `rating` | 0.0 to 5.0 scale |
| `reviewCount` | `reviewCount` | Number of reviews |
| `description` | `description` | Product description |
| `imageUrl` / `imageUrls[0]` | `imageUrl` | First image URL |
| `productUrl` | `productUrl` | Affiliate tracking URL |
| `specifications` | `specs` | Key-value map |
| `tags` | `tags` | List of tags |
| `highlights` | `highlights` | Key features |
| `inStock` / `available` | `availability` | Boolean |

---

## Performance Considerations

### Request Timeout

- Default: 5000ms (5 seconds)
- Configurable via `FLIPKART_API_TIMEOUT`
- Prevents long-running requests from blocking the application

### Pagination

- Default page size: 20 products
- Configurable via query parameter
- Reduces API bandwidth and improves response time

### Caching (Future Enhancement)

Planned features:
- Redis cache for API responses
- TTL: 1 hour for product data
- Cache key: query parameters hash

---

## Monitoring & Observability

### Application Logs

```
INFO: FlipkartProductProvider: discovering products for query='laptop', category='null'
INFO: FlipkartApiClient: sending GET request to [sanitized URL]
INFO: FlipkartApiClient: received successful response from Flipkart API
INFO: FlipkartProductProvider: successfully normalized 10 products from Flipkart API
INFO: ProductDiscoveryService: aggregated 25 total raw products from 2 providers
```

### Error Logs

```
ERROR: FlipkartApiClient: request failed — Connection timeout
WARN: FlipkartProductProvider: empty or null response from Flipkart API
ERROR: FlipkartProductProvider: error during product discovery — Network error. Returning empty result.
```

---

## Future Enhancements

### Planned Features

1. **Caching Layer**: Redis integration for API response caching
2. **Rate Limiting**: Track and respect Flipkart API rate limits
3. **Retry Logic**: Exponential backoff for failed requests
4. **Health Monitoring**: Provider health dashboard
5. **A/B Testing**: Compare performance across providers
6. **Advanced Deduplication**: Match products across different provider SKUs

### Additional Providers

- Amazon Creators API (global with India support)
- Shopify Global Catalog API (billions of products)
- eBay Browse API (auction/marketplace)
- Additional India retailers

---

## Support & Resources

### Official Documentation

- Flipkart Affiliate Program: https://affiliate.flipkart.com
- Flipkart API Docs: https://affiliate.flipkart.com/api-docs
- ShopSmart AI Architecture: [PRODUCT_DISCOVERY_ARCHITECTURE.md](./PRODUCT_DISCOVERY_ARCHITECTURE.md)

### Internal Documentation

- [PRODUCT_DISCOVERY_ARCHITECTURE.md](./PRODUCT_DISCOVERY_ARCHITECTURE.md) - Overall architecture
- [README.md](../README.md) - Project setup and deployment

---

## Summary Checklist

Before deploying to production:

- [ ] Register for Flipkart Affiliate account
- [ ] Generate API token
- [ ] Set environment variables (`FLIPKART_AFFILIATE_ID`, `FLIPKART_AFFILIATE_TOKEN`)
- [ ] Enable provider (`FLIPKART_PROVIDER_ENABLED=true`)
- [ ] Run tests (`mvn test`)
- [ ] Verify application starts successfully
- [ ] Test API endpoint with real query
- [ ] Verify products have `source: "FLIPKART"`
- [ ] Check logs for successful integration
- [ ] Monitor API usage in Flipkart dashboard
- [ ] Set up alerts for API failures (optional)

---

**Last Updated**: Phase 4 Step 3B
**Version**: 1.0.0
**Status**: ✅ Production Ready (requires credentials)
