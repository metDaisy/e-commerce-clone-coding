import React, { useState } from "react";
import "./product.css"; // CSS 파일을 별도 파일로 분리하는 것을 권장합니다.

interface ProductData {
  title: string;
  rating: number;
  reviewCount: number;
  price: string;
  deliveryDate: string;
  deliveryDetails: string;
  features: string[];
  mainImage: string;
  thumbnails: string[];
}

const Product: React.FC = () => {
  const [selectedImage, setSelectedImage] = useState<string>("");
  const [quantity, setQuantity] = useState<number>(1);
  const [searchQuery, setSearchQuery] = useState<string>("");

  // 실제 구현 시에는 API에서 데이터를 가져오거나 props로 받을 수 있습니다.
  const product: ProductData = {
    title:
      "Holikme Screen Cleaner Vacuum Attachment Remover with LED Light for iPhone iPad Samsung Galaxy S20 S10 S9 S8 Plus Note 20 10 Tablet PC Laptop Computer Screen Dust Removal Tool",
    rating: 4.4,
    reviewCount: 1234,
    price: "$12.99",
    deliveryDate: "Monday, July 24",
    deliveryDetails: "on orders shipped by Amazon over $35",
    features: [
      "Effective Cleaning: The vacuum cleaner removes dust, oil stains, water stains, fingerprints and other impurities on the screen, keeping the screen clean and clear.",
      "LED Light: Built-in LED light allows you to see the dust and dirt clearly during cleaning, ensuring thorough cleaning.",
      "Universal Compatibility: Works with iPhone, iPad, Samsung Galaxy series, tablets, laptops, and other electronic devices with screens.",
      "Easy to Use: Simply turn on the device and gently move it across the screen surface. No liquids or chemicals needed.",
      "Safe Material: Made of high-quality ABS plastic and safe materials, no scratching or damaging your device screen.",
    ],
    mainImage: "https://placehold.co/400x400/cccccc/333333?text=Product+Image",
    thumbnails: [
      "https://placehold.co/70x70/cccccc/333333?text=1",
      "https://placehold.co/70x70/cccccc/333333?text=2",
      "https://placehold.co/70x70/cccccc/333333?text=3",
      "https://placehold.co/70x70/cccccc/333333?text=4",
    ],
  };

  // 초기 로딩 시 메인 이미지를 썸네일 중 첫 번째로 설정 (또는 API에서 받은 값)
  useState(() => {
    setSelectedImage(product.mainImage);
  });

  const handleThumbnailClick = (img: string) => {
    setSelectedImage(img);
  };

  const handleQuantityChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setQuantity(Number(e.target.value));
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(e.target.value);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log("Searching for:", searchQuery);
    // 검색 로직 구현
  };

  const handleBuyNow = () => {
    console.log("Buying", quantity, "items");
    // 구매 로직 구현
  };

  const handleAddToCart = () => {
    console.log("Adding", quantity, "items to cart");
    // 장바구니 로직 구현
  };

  return (
    <div className="app-container">
      {/* Header */}
      <header className="header">
        <div className="logo-container">
          <div className="logo">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 100 20"
              fill="none"
            >
              <path d="M10 10h80v10H10z" fill="#FF9900" />
              <path d="M0 0h100v10H0z" fill="#146EB4" />
            </svg>
          </div>
        </div>

        <div className="deliver-to">
          <span className="icon">📍</span>
          <div className="deliver-text">
            <div className="small-text">Deliver to</div>
            <div className="bold-text">Korea</div>
          </div>
        </div>

        <form className="search-bar" onSubmit={handleSearchSubmit}>
          <select className="search-select">
            <option>All</option>
            <option>Electronics</option>
            <option>Home</option>
            <option>Industrial</option>
          </select>
          <input
            type="text"
            className="search-input"
            placeholder="Search Amazon"
            value={searchQuery}
            onChange={handleSearchChange}
          />
          <button type="submit" className="search-button">
            🔍
          </button>
        </form>

        <div className="nav-right">
          <div className="nav-option">
            <div className="small-text">Hello, Sign in</div>
            <div className="bold-text">Account & Lists</div>
          </div>
          <div className="nav-option">
            <div className="small-text">Returns</div>
            <div className="bold-text">& Orders</div>
          </div>
          <div className="nav-cart">
            <span className="cart-icon">🛒</span>
            <div className="bold-text">Cart</div>
          </div>
        </div>
      </header>

      {/* Main Product Content */}
      <main className="product-page">
        <div className="product-images">
          <img src={selectedImage} alt="Product Main" className="main-image" />
          <div className="thumbnail-container">
            {product.thumbnails.map((thumb, index) => (
              <img
                key={index}
                src={thumb}
                alt={`Thumbnail ${index + 1}`}
                className="thumbnail"
                onClick={() => handleThumbnailClick(thumb)}
              />
            ))}
          </div>
        </div>

        <div className="product-details">
          <h1 className="product-title">{product.title}</h1>

          <div className="product-rating">
            <div className="stars">
              {"★".repeat(Math.floor(product.rating))}
              {"☆".repeat(5 - Math.floor(product.rating))}
            </div>
            <div className="rating-count">
              {product.rating} out of 5 stars (
              {product.reviewCount.toLocaleString()} ratings)
            </div>
          </div>

          <div className="product-price">{product.price}</div>

          <div className="delivery-info">
            <div className="delivery-date">FREE delivery</div>
            <div className="delivery-details">
              {product.deliveryDate} {product.deliveryDetails}
            </div>
          </div>

          <div className="quantity-selector">
            <span className="quantity-label">Quantity:</span>
            <select value={quantity} onChange={handleQuantityChange}>
              {[1, 2, 3, 4, 5].map((num) => (
                <option key={num} value={num}>
                  {num}
                </option>
              ))}
            </select>
          </div>

          <div className="buy-buttons">
            <button className="buy-now" onClick={handleBuyNow}>
              Buy Now
            </button>
            <button className="add-to-cart" onClick={handleAddToCart}>
              Add to Cart
            </button>
          </div>

          <div className="prime-badge">
            <div className="prime-check">✅</div>
            <div className="prime-logo">FREE Returns</div>
          </div>

          <div className="product-details-section">
            <h3 className="section-title">About this item</h3>
            <ul className="feature-list">
              {product.features.map((feature, index) => (
                <li key={index}>{feature}</li>
              ))}
            </ul>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="footer">
        <div
          className="back-to-top"
          onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
        >
          Back to Top
        </div>
        <div className="footer-links">
          <div className="footer-column">
            <h4>Get to Know Us</h4>
            <ul>
              <li>Careers</li>
              <li>Blog</li>
              <li>About Amazon</li>
              <li>Investor Relations</li>
              <li>Amazon Devices</li>
            </ul>
          </div>
          <div className="footer-column">
            <h4>Make Money with Us</h4>
            <ul>
              <li>Sell products on Amazon</li>
              <li>Sell on Amazon Business</li>
              <li>Sell apps on Amazon</li>
              <li>Become an Affiliate</li>
              <li>Advertise Your Products</li>
            </ul>
          </div>
          <div className="footer-column">
            <h4>Amazon Payment Products</h4>
            <ul>
              <li>Amazon Business Card</li>
              <li>Shop with Points</li>
              <li>Reload Your Balance</li>
              <li>Amazon Currency Converter</li>
            </ul>
          </div>
          <div className="footer-column">
            <h4>Let Us Help You</h4>
            <ul>
              <li>Amazon and COVID-19</li>
              <li>Your Account</li>
              <li>Your Orders</li>
              <li>Shipping Rates & Policies</li>
              <li>Returns & Replacements</li>
            </ul>
          </div>
        </div>
        <div className="copyright">
          © 1996-2023, Amazon.com, Inc. or its affiliates
        </div>
      </footer>
    </div>
  );
};

export default Product;
