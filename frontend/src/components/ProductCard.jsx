import React, { useState } from 'react';
import { ShoppingBag } from 'lucide-react';

/**
 * ProductCard — Redesigned for mobile-first robustness
 *
 * Props:
 *   product    {id, name, price, costPrice, stock, unit, image, variants, wholesaleEnabled, wholesalePrices}
 *   onAdd      (product) => void   — produk biasa: langsung ke keranjang
 *   onVariant  (product, variants[]) => void — produk varian: buka modal pilih varian
 *   quantity   number | undefined  — jumlah item di keranjang saat ini (untuk tampilkan qty)
 */

const parseVariants = (p) => {
  if (!p?.variants) return [];
  try {
    const arr = typeof p.variants === 'string' ? JSON.parse(p.variants) : p.variants;
    return Array.isArray(arr) ? arr.map((v, i) => ({ id: v.id ?? i, ...v })) : [];
  } catch { return []; }
};

export default function ProductCard({ product, onAdd, onVariant, quantity = 0 }) {
  const [hovered, setHovered] = useState(false);

  const variants = parseVariants(product);
  const hasVariants = variants.length > 0;

  // ── Safe stock parsing: handles undefined/null/string/NaN from any browser ──
  const stockVal = (() => {
    if (!product) return 0;
    // Support both 'stock' and legacy 'stok' field names
    let raw = product.stock !== undefined && product.stock !== null
      ? product.stock
      : (product.stok !== undefined && product.stok !== null ? product.stok : null);
    if (raw === null) return 0;
    const n = Number(raw);
    return isNaN(n) ? 0 : n;
  })();

  // Whether stock data is actually present (distinguishes "loaded as 0" vs "not yet loaded")
  const stockIsKnown = product && (
    product.stock !== undefined || product.stok !== undefined
  );

  const isOut = !hasVariants && stockVal < 1;

  const hasImage =
    product?.image &&
    typeof product.image === 'string' &&
    product.image.trim() !== '' &&
    product.image !== 'null' &&
    product.image !== 'undefined';

  const handleClick = () => {
    if (isOut) return;
    if (hasVariants) {
      onVariant?.(product, variants);
    } else {
      onAdd?.(product);
    }
  };

  // ── Stock badge label — always computed, never conditionally skipped ──
  const stockLabel = (() => {
    if (!stockIsKnown) return '⏳ Memuat...';
    if (hasVariants) return `🎨 ${variants.length} Varian`;
    if (stockVal <= 0) return '🚫 Stok Habis';
    if (stockVal <= 5) return `⚠️ Sisa ${stockVal} ${product.unit || 'pcs'}`;
    return `✓ ${stockVal} ${product.unit || 'pcs'}`;
  })();

  const stockBg = (() => {
    if (!stockIsKnown) return '#F3F4F6';
    if (hasVariants) return '#EEF2FF';
    if (stockVal <= 0) return '#FEE2E2';
    if (stockVal <= 5) return '#FEF3C7';
    return '#DCFCE7';
  })();

  const stockColor = (() => {
    if (!stockIsKnown) return '#9CA3AF';
    if (hasVariants) return '#4F46E5';
    if (stockVal <= 0) return '#DC2626';
    if (stockVal <= 5) return '#D97706';
    return '#16A34A';
  })();

  const btnLabel = hasVariants
    ? 'Pilih Varian ›'
    : quantity > 0
    ? `+ Tambah (${quantity})`
    : '+ Keranjang';

  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: '#fff',
        borderRadius: 14,
        boxShadow: hovered ? '0 8px 20px rgba(0,0,0,0.12)' : '0 2px 8px rgba(0,0,0,0.07)',
        // NOTE: No overflow:hidden here — that was clipping the stock badge on Safari iOS
        border: isOut ? '1.5px solid #FCA5A5' : '1px solid #E2E8F0',
        display: 'flex',
        flexDirection: 'column',
        transition: 'transform 0.15s, box-shadow 0.15s',
        transform: hovered && !isOut ? 'translateY(-2px)' : 'translateY(0)',
        cursor: isOut ? 'not-allowed' : 'pointer',
        WebkitTapHighlightColor: 'transparent',
        minWidth: 0, // prevent flex children from overflowing
        width: '100%',
      }}
      onClick={handleClick}
    >
      {/* ── Image Box ─────────────────────────────────────────── */}
      <div style={{
        width: '100%',
        aspectRatio: '1 / 1',
        background: '#F8FAFC',
        borderRadius: '14px 14px 0 0',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
        overflow: 'hidden', // OK here — image-only container
        flexShrink: 0,
      }}>
        {hasImage ? (
          <img
            src={product.image}
            alt={product.name}
            draggable="false"
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'contain',
              padding: 6,
              transition: 'transform 0.3s',
              transform: hovered ? 'scale(1.04)' : 'scale(1)',
            }}
          />
        ) : (
          <ShoppingBag size={36} color="#CBD5E1" />
        )}

        {/* Habis overlay */}
        {isOut && (
          <div style={{
            position: 'absolute', inset: 0,
            background: 'rgba(239,68,68,0.12)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <span style={{
              background: '#EF4444', color: '#fff',
              fontSize: 9, fontWeight: 800,
              padding: '2px 8px', borderRadius: 99,
            }}>HABIS</span>
          </div>
        )}

        {/* Cart qty badge */}
        {quantity > 0 && (
          <div style={{
            position: 'absolute', top: 6, left: 6,
            background: '#4F46E5', color: '#fff',
            fontSize: 10, fontWeight: 800,
            width: 22, height: 22, borderRadius: '50%',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 2px 6px rgba(79,70,229,0.5)',
          }}>
            {quantity}
          </div>
        )}
      </div>

      {/* ── Product Info ─────────────────────────────────────── */}
      <div style={{
        padding: '8px 10px 10px',
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        minHeight: 0, // allow flex shrink
        gap: 3,
      }}>

        {/* Nama — clamp font so it never blows layout */}
        <div style={{
          fontWeight: 700,
          fontSize: 'clamp(11px, 3vw, 13px)',
          color: '#1E293B',
          lineHeight: 1.3,
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden',
          wordBreak: 'break-word',
        }}>
          {product?.name}
        </div>

        {/* Harga */}
        <div style={{
          color: '#4F46E5',
          fontWeight: 800,
          fontSize: 'clamp(12px, 3.2vw, 14px)',
        }}>
          Rp {Number(product?.price || 0).toLocaleString('id-ID')}
        </div>

        {/* ── Stock badge — ALWAYS rendered, never conditionally hidden ── */}
        <div style={{ flexShrink: 0, marginTop: 2 }}>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              background: stockBg,
              color: stockColor,
              fontSize: 'clamp(9px, 2.5vw, 11px)',
              fontWeight: 700,
              padding: '2px 7px',
              borderRadius: 99,
              whiteSpace: 'nowrap',
              lineHeight: '1.4',
              maxWidth: '100%',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {stockLabel}
          </span>
        </div>

      </div>
    </div>
  );
}
