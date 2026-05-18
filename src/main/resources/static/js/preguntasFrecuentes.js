   function toggleFAQ(button) {
            const faqItem = button.closest('.faq-item');
            const content = faqItem.querySelector('.faq-content');
            const icon = button.querySelector('svg');
            const isExpanded = button.getAttribute('aria-expanded') === 'true';

            // Close all other FAQs
            document.querySelectorAll('.faq-item').forEach(item => {
                if (item !== faqItem) {
                    const otherContent = item.querySelector('.faq-content');
                    const otherButton = item.querySelector('button');
                    const otherIcon = otherButton.querySelector('svg');
                    
                    otherContent.style.maxHeight = '0px';
                    otherButton.setAttribute('aria-expanded', 'false');
                    otherIcon.style.transform = 'rotate(0deg)';
                }
            });

            // Toggle current FAQ
            if (!isExpanded) {
                content.style.maxHeight = content.scrollHeight + 'px';
                button.setAttribute('aria-expanded', 'true');
                icon.style.transform = 'rotate(180deg)';
            } else {
                content.style.maxHeight = '0px';
                button.setAttribute('aria-expanded', 'false');
                icon.style.transform = 'rotate(0deg)';
            }
        }

        