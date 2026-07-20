import './ProductGrid.css';

function ProductGrid({ cards }) {
  return (
    <div className="product-grid" id="product-grid">
      {cards.map((card, idx) => (
        <div className="product-card" key={idx} id={`product-card-${idx}`}>
          <h3 className="product-card__title">{card.title}</h3>
          <div className="product-card__items">
            {card.items.map((item, i) => (
              <div className="product-card__item" key={i}>
                <div className="product-card__emoji">{item.emoji}</div>
                <span className="product-card__item-name">{item.name}</span>
              </div>
            ))}
          </div>
          <a href="#" className="product-card__link">{card.link}</a>
        </div>
      ))}
    </div>
  );
}

export default ProductGrid;
