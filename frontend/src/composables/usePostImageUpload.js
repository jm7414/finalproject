import axios from 'axios';

export const usePostImageUpload = () => {

    const upload = async (selectedFile) => {
        
        return new Promise(async (resolve, reject) => {
            console.log(`게시물 이미지 업로드 -> name: ${selectedFile.name}`);

            try { 
                const formData = new FormData();
                formData.append("file", selectedFile);
            
                const response = await axios.post(
                    `/api/upload/post-image`,
                    formData, 
                    { withCredentials: true }
                );
        
                // 서버가 돌려준 JSON에서 이미지 URL만 꺼내서 반환
                resolve(response.data.imageUrl);
        
            } catch(err) {
                console.error(`이미지 업로드 에러 -> ${err}`);
                reject(err);
            }
        });
    }

    return { upload }
}