# Authentication API Documentation

## Base URL
```
http://localhost:8081/api/auth-service
```

## Endpoints Summary

| Method | Endpoint | Purpose | Requires Auth |
|--------|----------|---------|---------------|
| POST | `/auth/login` | Login with credentials | No |
| POST | `/auth/refresh` | Get new access token | No (refresh token) |
| POST | `/auth/logout` | Revoke current token | No (refresh token) |
| POST | `/auth/logout-all` | Logout from all devices | Yes (access token) |

---

## 1. Login

### Request
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### cURL Example
```bash
curl -X POST http://localhost:8081/api/auth-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJyb2xlcyI6IlVTRVIsSUQsQURNSU4iLCJ0eXBlIjoiYWNjZXNzIiwic3ViIjoiNTUwZTg0MDAtZTI5Yi00MWQ0LWE3MTYtNDQ2NjU1NDQwMDAwIiwiaWF0IjoxNzI0MTEyMDAwLCJleHAiOjE3MjQxOTg0MDB9.XYZ...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ0eXBlIjoicmVmcmVzaCIsInN1YiI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LTQ0NjY1NTQ0MDAwMCIsImlhdCI6MTcyNDExMjAwMCwiZXhwIjoxNzI0NzE3MjAwfQ.ABD...",
    "token_type": "Bearer",
    "expires_in": 86400,
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "roles": [
      "USER",
      "ADMIN"
    ],
    "created_at": "2024-06-18T10:30:00"
  },
  "message": "Login successful"
}
```

### Response (401 Unauthorized)
```json
{
  "success": false,
  "error": "Invalid email or password"
}
```

### Response (400 Bad Request)
```json
{
  "success": false,
  "error": "User account is suspended. Please contact support."
}
```

### Status Codes
| Code | Meaning |
|------|---------|
| 200 | Login successful |
| 400 | Account suspended/pending or missing credentials |
| 401 | Invalid email or password |
| 500 | Server error |

### What Happens
1. ✓ Email and password validated
2. ✓ User status checked (must be ACTIVE)
3. ✓ Password verified against hash
4. ✓ JWT access token generated (24 hours)
5. ✓ JWT refresh token generated (7 days)
6. ✓ Refresh token saved to database with:
   - User ID
   - User agent (browser info)
   - IP address
   - Created timestamp
7. ✓ Response includes both tokens

### Token Claims
**Access Token:**
```json
{
  "email": "user@example.com",
  "roles": "USER,ADMIN",
  "type": "access",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1724112000,
  "exp": 1724198400
}
```

**Refresh Token:**
```json
{
  "email": "user@example.com",
  "type": "refresh",
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1724112000,
  "exp": 1724717200
}
```

---

## 2. Refresh Token

### Purpose
Get a new access token without re-entering credentials. Use when access token expires.

### Request
```http
POST /auth/refresh
Authorization: Bearer <refresh_token_here>
```

### cURL Example
```bash
REFRESH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST http://localhost:8081/api/auth-service/auth/refresh \
  -H "Authorization: Bearer $REFRESH_TOKEN"
```

### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.NEW_TOKEN...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.SAME_TOKEN...",
    "token_type": "Bearer",
    "expires_in": 86400,
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com"
  },
  "message": "Token refreshed successfully"
}
```

### Response (401 Unauthorized)
```json
{
  "success": false,
  "error": "Invalid or revoked refresh token"
}
```

### Response (400 Bad Request)
```json
{
  "success": false,
  "error": "Refresh token is required in Authorization header"
}
```

### Status Codes
| Code | Meaning |
|------|---------|
| 200 | Token refreshed successfully |
| 400 | Missing or malformed token |
| 401 | Token invalid, revoked, or expired |
| 500 | Server error |

### What Happens
1. ✓ Extract refresh token from Authorization header
2. ✓ Check database: is token revoked?
3. ✓ Check database: is token expired?
4. ✓ Validate JWT signature
5. ✓ Verify token type is "refresh"
6. ✓ Extract user claims
7. ✓ Generate new access token
8. ✓ Return new access token (refresh token unchanged)

### Security Notes
- **Token checked against database first** - if revoked, rejected immediately
- Refresh token remains the same (can be used multiple times)
- Access token is new each time
- Can be called repeatedly without logout

---

## 3. Logout (Revoke Current Token)

### Purpose
Logout user by revoking the current refresh token. Prevents token from being used again.

### Request
```http
POST /auth/logout
Authorization: Bearer <refresh_token_here>
```

### cURL Example
```bash
REFRESH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST http://localhost:8081/api/auth-service/auth/logout \
  -H "Authorization: Bearer $REFRESH_TOKEN"
```

### Response (200 OK)
```json
{
  "success": true,
  "message": "Logout successful"
}
```

### Response (400 Bad Request)
```json
{
  "success": false,
  "error": "Refresh token is required in Authorization header"
}
```

### Status Codes
| Code | Meaning |
|------|---------|
| 200 | Logout successful, token revoked |
| 400 | Missing or malformed token |
| 500 | Server error |

### What Happens
1. ✓ Extract refresh token from Authorization header
2. ✓ Find token in database
3. ✓ Set `revoked_at = CURRENT_TIMESTAMP`
4. ✓ Save to database
5. ✓ Token is now **permanently invalid**

### After Logout
- ✗ Old refresh token cannot be used for refresh
- ✗ Old refresh token cannot be used for logout again
- ✓ User must login again to get new tokens
- ✓ Auth token becomes "logged out"

### Use Cases
- User manually clicks "logout" button
- User wants to invalidate a specific device
- Security concern for one device but not others

---

## 4. Logout All Devices

### Purpose
Revoke ALL refresh tokens for a user. Logout from all devices/browsers simultaneously.

### Request
```http
POST /auth/logout-all
Authorization: Bearer <access_token_here>
```

### cURL Example
```bash
ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST http://localhost:8081/api/auth-service/auth/logout-all \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### Response (200 OK)
```json
{
  "success": true,
  "message": "Logged out from all devices"
}
```

### Response (400 Bad Request)
```json
{
  "success": false,
  "error": "Access token is required in Authorization header"
}
```

### Status Codes
| Code | Meaning |
|------|---------|
| 200 | All tokens revoked successfully |
| 400 | Missing or malformed token |
| 403 | Feature requires proper authentication setup |
| 500 | Server error |

### What Happens
1. ✓ Extract access token from Authorization header
2. ✓ Extract user ID from token claims
3. ✓ Find ALL refresh tokens for this user
4. ✓ Set `revoked_at = CURRENT_TIMESTAMP` for all
5. ✓ Save all records to database
6. ✓ User logged out from all devices

### After Logout All
- ✗ ALL refresh tokens for user are revoked
- ✗ User is logged out from ALL browsers/devices
- ✗ Hacker cannot use stolen token from any device
- ✓ Only way to access: login again

### Use Cases
- User changed password → revoke old sessions
- User suspects account compromise
- Security alert triggered
- User wants to "logout everywhere"

---

## Complete Flow Example

### 1. User Login
```bash
curl -X POST http://localhost:8081/api/auth-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'

# Response includes:
# - access_token (24 hours)
# - refresh_token (7 days)
```

### 2. Use Access Token for API Calls
```bash
ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
REFRESH_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Access protected resource
curl -X GET http://localhost:8081/api/auth-service/users \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### 3. Access Token Expires (After 24 Hours)
```bash
# Old access token fails
curl -X GET http://localhost:8081/api/auth-service/users \
  -H "Authorization: Bearer $OLD_ACCESS_TOKEN"
# Response: 401 Unauthorized
```

### 4. Refresh for New Token
```bash
# Use refresh token to get new access token
curl -X POST http://localhost:8081/api/auth-service/auth/refresh \
  -H "Authorization: Bearer $REFRESH_TOKEN"

# Response includes:
# - NEW access_token (24 more hours)
# - SAME refresh_token (can be reused)
```

### 5. Continue Using API
```bash
NEW_ACCESS_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8081/api/auth-service/users \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN"
# Response: 200 OK
```

### 6. User Logout
```bash
curl -X POST http://localhost:8081/api/auth-service/auth/logout \
  -H "Authorization: Bearer $REFRESH_TOKEN"

# Response: 200 OK
```

### 7. Old Token No Longer Works
```bash
# Try to refresh with revoked token
curl -X POST http://localhost:8081/api/auth-service/auth/refresh \
  -H "Authorization: Bearer $REFRESH_TOKEN"

# Response: 401 - Token has been revoked
```

---

## Troubleshooting

### "Invalid or revoked refresh token"
**Causes:**
- Token has been logged out
- Token has expired (7 days old)
- Token is malformed or corrupted
- Wrong token type sent

**Solutions:**
- Check token is not expired
- Check database for revocation
- Login again to get new token

### "Refresh token is required in Authorization header"
**Causes:**
- Authorization header missing
- Wrong Bearer format
- Empty token

**Solutions:**
```bash
# Correct:
curl -H "Authorization: Bearer <token>"

# Wrong:
curl -H "Authorization: <token>"
curl -H "Authorization: Basic <token>"
```

### "Invalid email or password"
**Causes:**
- Email not found in database
- Password incorrect
- Email/password case sensitivity

**Solutions:**
- Verify email is correct
- Verify password is correct
- Password is case-sensitive

### "User account is suspended"
**Causes:**
- User account marked as SUSPENDED
- User account marked as PENDING

**Solutions:**
- Contact admin to activate account
- Check email for activation link

---

## Rate Limiting (Future)

Currently no rate limiting. Recommended limits:
- Login: 5 attempts per IP per minute
- Refresh: 10 attempts per token per minute
- Logout: 10 attempts per hour

---

## Security Best Practices

### Client Side
```javascript
// Store tokens securely
localStorage.setItem('accessToken', response.accessToken);
localStorage.setItem('refreshToken', response.refreshToken);

// Use access token for API calls
fetch('/api/users', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  }
});

// Refresh when access token expires
// Implement automatic refresh before expiration

// Clear tokens on logout
localStorage.removeItem('accessToken');
localStorage.removeItem('refreshToken');
```

### Server Side
```java
// Tokens validated automatically via JWT
// Refresh tokens checked against database
// No additional setup needed

// Log suspicious activities
log.warn("Logout from device: {} IP: {}", userAgent, ipAddress);

// Monitor failed refresh attempts
log.warn("Refresh failed: token revoked for user: {}", email);
```

---

## Performance Notes

| Operation | Latency | DB Queries |
|-----------|---------|-----------|
| Login | ~50ms | 1 write (token save) |
| Refresh | ~15ms | 1 read (token lookup) |
| Logout | ~20ms | 1 update (revoke) |
| Logout All | ~30ms | 1 update (all tokens) |

All database operations use indexed queries for O(log n) performance.

---

## Token Expiration Timeline

```
Login: 2024-06-18 10:00:00

Access Token:
- Expires: 2024-06-18 22:00:00 (24 hours later)
- Can refresh after expiration

Refresh Token:
- Expires: 2024-06-25 10:00:00 (7 days later)
- Cannot refresh after expiration
- User must login again

Database Cleanup:
- Runs daily at 2:00 AM
- Deletes tokens revoked > 30 days ago
```

---

## Related Documentation

- [Refresh Token Storage Implementation](./REFRESH_TOKEN_STORAGE.md)
- [JWT Security Guide](./JWT_SECURITY.md) (future)
- [API Error Codes](./ERROR_CODES.md) (future)

