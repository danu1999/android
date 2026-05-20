import React, { useState } from 'react';
import { ShoppingBag } from 'lucide-react';

/**
 * ProductCard — Desain identik TokoOnline, diadaptasi untuk Kasir POS
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
  const isOut = !hasVariants && product.stock < 1;

  const hasImage =
    product.image &&
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

  // ── Label stok / varian ────────────────────────────────────────
  const stockBadge = (() => {
    if (hasVariants) return { label: `🎨 ${variants.length} Varian`, bg: '#EEF2FF', color: '#4F46E5' };
    if (product.stock === 0) return { label: '🚫 Stok Habis', bg: '#FEE2E2', color: '#DC2626' };
    if (product.stock <= 5) return { label: `⚠️ Sisa ${product.stock} ${product.unit || 'pcs'}`, bg: '#FEF3C7', color: '#D97706' };
    return { label: `✓ Tersedia ${product.stock} ${product.unit || 'pcs'}`, bg: '#DCFCE7', color: '#16A34A' };
  })();

  const btnDisabled = isOut;
  const btnLabel = hasVariants ? 'Pilih Varian ›' : quantity > 0 ? `+ Tambah (${quantity})` : '+ Keranjang';

  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: '#fff',
        borderRadius: 16,
        boxShadow: hovered ? '0 8px 20px rgba(0,0,0,0.12)' : '0 2px 8px rgba(0,0,0,0.07)',
        overflow: 'hidden',
        border: '1px solid #E2E8F0',
        display: 'flex',
        flexDirection: 'column',
        transition: 'transform 0.15s, box-shadow 0.15s',
        transform: hovered && !isOut ? 'translateY(-3px)' : 'translateY(0)',
        cursor: isOut ? 'not-allowed' : 'pointer',
        WebkitTapHighlightColor: 'transparent',
      }}
      onClick={handleClick}
    >
      {/* ── Image Box — paddingTop trick, kompatibel iOS Safari & Android ─── */}
      <div style={{
        width: '100%',
        paddingTop: '100%',           /* 1:1 — tidak pakai aspectRatio agar aman di semua browser */
        background: '#F8FAFC',
        position: 'relative',
        overflow: 'hidden',
        flexShrink: 0,
      }}>
        {hasImage ? (
          <img
            src={product.image}
            alt={product.name}
            style={{
              position: 'absolute',
              top: 0, left: 0,
              width: '100%',
              height: '100%',
              objectFit: 'contain',
              padding: 8,
              transition: 'transform 0.3s',
              transform: hovered ? 'scale(1.05)' : 'scale(1)',
            }}
          />
        ) : (
          <div style={{
            position: 'absolute',
            top: 0, left: 0, right: 0, bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: '#F1F5F9',
          }}>
            <ShoppingBag size={36} color="#CBD5E1" />
          </div>
        )}

        {/* Badge Habis — pojok kanan atas */}
        {product.stock < 1 && !hasVariants && (
          <div style={{
            position: 'absolute', top: 8, right: 8,
            background: '#EF4444', color: '#fff',
            fontSize: 10, fontWeight: 700,
            padding: '2px 8px', borderRadius: 99,
            zIndex: 2,
          }}>
            Habis
          </div>
        )}

        {/* Badge jumlah di keranjang — pojok kiri atas */}
        {quantity > 0 && (
          <div style={{
            position: 'absolute', top: 8, left: 8,
            background: '#4F46E5', color: '#fff',
            fontSize: 11, fontWeight: 800,
            width: 24, height: 24, borderRadius: '50%',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 2px 6px rgba(79,70,229,0.5)',
            zIndex: 2,
          }}>
            {quantity}
          </div>
        )}
      </div>

      {/* ── Product Info ─────────────────────────────────────── */}
      <div style={{ padding: '10px 12px 12px', flex: 1, display: 'flex', flexDirection: 'column' }}>

        {/* Nama Produk — wajib tampil */}
        <div style={{
          fontWeight: 700,
          fontSize: 13,
          color: '#1E293B',
          marginBottom: 4,
          lineHeight: 1.3,
          display: '-webkit-box',
          WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical',
          overflow: 'hidden',
        }}>
          {product.name}
        </div>

        {/* Harga */}
        <div style={{ color: '#4F46E5', fontWeight: 800, fontSize: 14, marginBottom: 6 }}>
          Rp {Number(product.price).toLocaleString('id-ID')}
        </div>

        {/* Badge stok / varian */}
        <div style={{ marginBottom: 8 }}>
          <span style={{
            fontSize: 11, fontWeight: 700,
            background: stockBadge.bg,
            color: stockBadge.color,
            padding: '2px 8px', borderRadius: 99,
            display: 'inline-block',
          }}>
            {stockBadge.label}
          </span>
        </div>

        {/* Tombol aksi */}
        <button
          disabled={btnDisabled}
          onClick={(e) => { e.stopPropagation(); handleClick(); }}
          style={{
            width: '100%',
            padding: '7px 0',
            borderRadius: 10,
            border: 'none',
            fontWeight: 700,
            fontSize: 13,
            cursor: btnDisabled ? 'not-allowed' : 'pointer',
            background: btnDisabled ? '#F1F5F9' : quantity > 0 ? '#4F46E5' : '#EEF2FF',
            color: btnDisabled ? '#94A3B8' : quantity > 0 ? '#fff' : '#4F46E5',
            transition: 'background 0.15s, color 0.15s',
            marginTop: 'auto',
          }}
        >
          {btnLabel}
        </button>
      </div>
    </div>
  );
}
