import React, { useState } from 'react';
import { Plus, ShoppingBag } from 'lucide-react';

/**
 * ProductCard — "Sushiban Style" POS Mobile Card
 * Requires Tailwind CSS v4
 *
 * Props:
 *   product   {id, name, price, stock, image, costPrice, unit, variants}
 *   onAdd     (product) => void   — simple product: adds to cart directly
 *   onVariant (product) => void   — variant product: opens customization modal
 */
export default function ProductCard({ product, onAdd, onVariant }) {
  const [pressed, setPressed] = useState(false);

  // Parse variants
  const hasVariants = product.variants && (() => {
    try {
      const v = typeof product.variants === 'string'
        ? JSON.parse(product.variants)
        : product.variants;
      return Array.isArray(v) && v.length > 0;
    } catch { return false; }
  })();

  const isOut  = !hasVariants && product.stock === 0;
  const isLow  = !hasVariants && product.stock > 0 && product.stock <= 5;
  const isGood = !hasVariants && product.stock > 10;

  const hasImage =
    product.image &&
    typeof product.image === 'string' &&
    product.image.trim() !== '' &&
    product.image !== 'null';

  const handleAdd = (e) => {
    e.stopPropagation();
    if (isOut) return;
    if (hasVariants) onVariant?.(product);
    else onAdd?.(product);
  };

  // Stock badge color classes
  const badgeColor = hasVariants
    ? 'text-indigo-600'
    : isLow
      ? 'text-amber-600'
      : isGood
        ? 'text-green-600'
        : 'text-slate-500';

  return (
    <div
      onClick={() => !isOut && (hasVariants ? onVariant?.(product) : onAdd?.(product))}
      className={[
        'group relative bg-white rounded-2xl overflow-hidden border border-indigo-50',
        'shadow-sm hover:shadow-md transition-all duration-200',
        isOut ? 'cursor-not-allowed opacity-75' : 'cursor-pointer hover:-translate-y-0.5',
      ].join(' ')}
    >
      {/* ── Image Area ─────────────────────────────────────── */}
      <div className="relative w-full" style={{ paddingTop: '100%' /* 1:1 square */ }}>

        {/* Image or placeholder */}
        {hasImage ? (
          <img
            src={product.image}
            alt={product.name}
            className="absolute inset-0 w-full h-full object-contain p-2 transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="absolute inset-0 flex items-center justify-center bg-indigo-50">
            <ShoppingBag size={32} className="text-indigo-200" />
          </div>
        )}

        {/* Glassmorphism Stock Badge — Top Left */}
        {!isOut && (
          <div className={[
            'absolute top-2 left-2 px-2.5 py-0.5 rounded-full',
            'bg-white/90 backdrop-blur-sm text-[10px] font-extrabold tracking-wide shadow-sm',
            badgeColor,
          ].join(' ')}>
            {hasVariants
              ? '🎨 Multi Varian'
              : isLow
                ? `⚠️ Sisa ${product.stock}`
                : `✓ ${product.stock} ${product.unit || 'pcs'}`}
          </div>
        )}

        {/* Out-of-stock overlay */}
        {isOut && (
          <div className="absolute inset-0 flex items-center justify-center bg-white/70 backdrop-blur-sm">
            <span className="bg-slate-500 text-white text-[11px] font-black tracking-widest px-4 py-1.5 rounded-lg">
              HABIS
            </span>
          </div>
        )}

        {/* Floating Action Button — seam between image and content */}
        <button
          disabled={isOut}
          onMouseDown={() => setPressed(true)}
          onMouseUp={() => setPressed(false)}
          onTouchStart={e => { e.stopPropagation(); setPressed(true); }}
          onTouchEnd={e => { e.stopPropagation(); setPressed(false); handleAdd(e); }}
          onClick={handleAdd}
          aria-label={hasVariants ? 'Pilih varian' : 'Tambah ke keranjang'}
          className={[
            'absolute bottom-0 right-2.5 z-10 translate-y-1/2',
            'w-9 h-9 rounded-full flex items-center justify-center',
            'shadow-lg transition-all duration-150',
            isOut
              ? 'bg-slate-300 cursor-not-allowed'
              : pressed
                ? 'bg-indigo-700 scale-90'
                : 'bg-indigo-600 hover:bg-indigo-700 active:scale-90',
          ].join(' ')}
        >
          <Plus size={18} strokeWidth={2.5} className="text-white" />
        </button>
      </div>

      {/* ── Content Area ───────────────────────────────────── */}
      <div className="px-3 pt-4 pb-3 pr-12" /* pr-12 = space for FAB */>

        {/* Product Name — 2-line clamp, Priority 1 */}
        <p
          className="font-bold text-[15px] text-slate-800 leading-snug mb-1.5"
          style={{
            display: '-webkit-box',
            WebkitLineClamp: 2,
            WebkitBoxOrient: 'vertical',
            overflow: 'hidden',
          }}
        >
          {product.name}
        </p>

        {/* Price Row */}
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="font-bold text-[13px] text-amber-600">
            Rp {Number(product.price).toLocaleString('id-ID')}
          </span>
          {hasVariants && (
            <span className="text-[10px] font-semibold text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded-md">
              • Opsi
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
